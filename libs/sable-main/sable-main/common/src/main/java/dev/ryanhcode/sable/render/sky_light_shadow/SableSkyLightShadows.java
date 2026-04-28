package dev.ryanhcode.sable.render.sky_light_shadow;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL30;

public class SableSkyLightShadows {

    public static final float SHADOW_VOLUME_SIZE = 256f / 2f;

    private static final ResourceLocation FRAMEBUFFER_NAME = Sable.sablePath("sub_level_shadow");
    private static final Matrix4f PROJECTION_MAT = new Matrix4f();
    private static final Vector3d SHADOW_CAMERA_POSITION = new Vector3d();
    private static final Quaternionf SHADOW_CAMERA_ORIENTATION = new Quaternionf();

    private static boolean isRenderingShadowMap = false;
    private static boolean isEnabled = false;

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        SableSkyLightShadows.isEnabled = isEnabled;
    }

    public static void renderShadowMap(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer levelRenderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack matrixStack, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick, final DeltaTracker deltaTracker, final Camera camera, final Frustum frustum) {
        if (!SableSkyLightShadows.isEnabled()) {
            return;
        }
        if (VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            return;
        }
        if (stage != VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        final AdvancedFbo fbo = getShadowsFramebuffer();


        if (fbo != null) {
            fbo.bind(true);
            GL30.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
            fbo.clear();

            final Minecraft client = Minecraft.getInstance();
            final Level level = client.level;
            final Window window = client.getWindow();

            final Matrix4f modelView = new Matrix4f();
            PROJECTION_MAT.identity().ortho(-SHADOW_VOLUME_SIZE, SHADOW_VOLUME_SIZE, -SHADOW_VOLUME_SIZE, SHADOW_VOLUME_SIZE, 0.5f, SHADOW_VOLUME_SIZE);

            // account for the smaller screen size
            final Vec3 cameraPosition = camera.getPosition();
            final Vec3 shadowCameraPosition = new Vec3(cameraPosition.x, cameraPosition.y + SHADOW_VOLUME_SIZE / 2.0f, cameraPosition.z);

            JOMLConversion.toJOML(shadowCameraPosition, SHADOW_CAMERA_POSITION);
            SHADOW_CAMERA_POSITION.set(Math.floor(SHADOW_CAMERA_POSITION.x), SHADOW_CAMERA_POSITION.y, Math.floor(SHADOW_CAMERA_POSITION.z));
            isRenderingShadowMap = true;
            VeilLevelPerspectiveRenderer.render(fbo, modelView, PROJECTION_MAT, SHADOW_CAMERA_POSITION, SHADOW_CAMERA_ORIENTATION.identity().rotateX((float) (Math.PI / 2)), SHADOW_VOLUME_SIZE / 16f, deltaTracker, false);
            isRenderingShadowMap = false;
        }
    }

    public static @Nullable AdvancedFbo getShadowsFramebuffer() {
        return VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(FRAMEBUFFER_NAME);
    }

    public static boolean renderingShadowMap() {
        return isRenderingShadowMap;
    }

    public static void bindShadowMapTexture(final ShaderInstance shader) {
        if (!SableSkyLightShadows.isEnabled()) {
            return;
        }

        final Uniform volumeSizeUniform = shader.getUniform(SableDynamicSkyLightShadowPreProcessor.SHADOW_VOLUME_SIZE_UNIFORM);
        if (volumeSizeUniform != null) {
            volumeSizeUniform.set(SableSkyLightShadows.SHADOW_VOLUME_SIZE);
        }

        final Uniform offsetUniform = shader.getUniform(SableDynamicSkyLightShadowPreProcessor.SHADOW_ORIGIN_UNIFORM);
        if (offsetUniform != null) {
            final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            offsetUniform.set((float) (SHADOW_CAMERA_POSITION.x - camera.x), (float) (SHADOW_CAMERA_POSITION.y - camera.y), (float) (SHADOW_CAMERA_POSITION.z - camera.z));
        }

        final AdvancedFbo fbo = getShadowsFramebuffer();
        shader.setSampler(SableDynamicSkyLightShadowPreProcessor.SAMPLER_NAME, fbo.getDepthTextureAttachment());
    }
}
