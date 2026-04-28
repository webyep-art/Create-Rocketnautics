package dev.ryanhcode.sable.render.water_occlusion;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegion;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.Set;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL32C.GL_DEPTH_CLAMP;

/**
 * Manages water occlusion rendering for sub-levels
 */
@ApiStatus.Internal
public class WaterOcclusionRenderer {
    private final Set<SimpleCulledRenderRegion> regions = new ObjectOpenHashSet<>();
    private AdvancedFbo closeBuffer;
    private AdvancedFbo farBuffer;
    private Level level;

    private static boolean isEnabled = false;

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        WaterOcclusionRenderer.isEnabled = isEnabled;
    }

    @Nullable
    @ApiStatus.Internal
    public SimpleCulledRenderRegion addRegion(final Collection<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return null;
        }

        final SimpleCulledRenderRegion region = new WaterOcclusionRenderRegion(blocks);
        this.regions.add(region);
        return region;
    }

    public void removeRegion(final SimpleCulledRenderRegion region) {
        region.free();
        this.regions.remove(region);
    }

    private void updateFramebuffers(final boolean needed) {
        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget renderTarget = minecraft.getMainRenderTarget();

        if (!needed && this.closeBuffer != null) {
            this.closeBuffer.free();
            this.farBuffer.free();
            this.closeBuffer = null;
            this.farBuffer = null;
        }

        if (needed && (this.closeBuffer == null || renderTarget.width != this.closeBuffer.getWidth() || renderTarget.height != this.farBuffer.getHeight())) {
            if (this.closeBuffer != null) {
                this.closeBuffer.free();
                this.farBuffer.free();
            }

            this.closeBuffer = AdvancedFbo.withSize(renderTarget.width, renderTarget.height).addColorTextureBuffer().setDepthTextureBuffer().build(true);
            this.farBuffer = AdvancedFbo.withSize(renderTarget.width, renderTarget.height).addColorTextureBuffer().setDepthTextureBuffer().build(true);
        }
    }

    public void preRenderTranslucent(final Matrix4f modelView, final Matrix4f projMat) {
        if (!isEnabled()) {
            return;
        }

        final WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(this.level);
        final boolean needed = !this.regions.isEmpty() || container == null;

        this.updateFramebuffers(needed);

        if (!needed) {
            return;
        }

        final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        this.closeBuffer.bind(true);

        // if we're inside any of the regions, we need to clear the depth to 0
        final boolean cameraOccluded = container.isOccluded(cameraPos);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        glEnable(GL_DEPTH_CLAMP);
        this.closeBuffer.clear(0.0F, 0.0F, 0.0F, 0.0F, cameraOccluded ? 0.0F : 1.0F, this.closeBuffer.getClearMask());
        glCullFace(GL_BACK);

        for (final SimpleCulledRenderRegion region : this.regions) {
            region.render(modelView, projMat);
        }
        AdvancedFbo.unbind();

        this.farBuffer.bind(true);
        this.farBuffer.clear();
        glCullFace(GL_FRONT);

        for (final SimpleCulledRenderRegion region : this.regions) {
            region.render(modelView, projMat);
        }
        AdvancedFbo.unbind();

        // reset state
        glCullFace(GL_BACK);
        glDisable(GL_DEPTH_CLAMP);
        ShaderProgram.unbind();
    }

    public void setupTranslucentShader(final ShaderInstance shader) {
        if (!isEnabled()) {
            return;
        }

        final Uniform uniform = shader.getUniform(SableWaterOcclusionPreProcessor.ENABLE_UNIFORM);

        if (this.closeBuffer == null) {
            if (uniform != null) {
                uniform.set(0.0F);
            }
            return;
        }

        final Window window = Minecraft.getInstance().getWindow();
        final Uniform screenSize = shader.getUniform("ScreenSize");
        if (screenSize != null) {
            screenSize.set((float) window.getWidth(), (float) window.getHeight());
        }
        if (uniform != null) {
            uniform.set(1.0F);
        }

        shader.setSampler(SableWaterOcclusionPreProcessor.CLOSE_SAMPLER_NAME, this.closeBuffer.getDepthTextureAttachment());
        shader.setSampler(SableWaterOcclusionPreProcessor.FAR_SAMPLER_NAME, this.farBuffer.getDepthTextureAttachment());
    }

    public void update() {
        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level != this.level) {
            this.level = minecraft.level;

            this.regions.forEach(SimpleCulledRenderRegion::free);
            this.regions.clear();
        }
    }

}
