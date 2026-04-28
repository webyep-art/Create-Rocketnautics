package dev.ryanhcode.sable.sublevel.render.sodium;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.sublevel_render.sodium.DefaultChunkRendererExtension;
import dev.ryanhcode.sable.mixinterface.sublevel_render.sodium.OcclusionCullerExtension;
import dev.ryanhcode.sable.mixinterface.sublevel_render.sodium.RenderSectionManagerExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import org.joml.*;

public class SubLevelRenderSectionManager extends RenderSectionManager {

    private final Vector3d chunkOffset = new Vector3d();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final ClientSubLevel subLevel;
    private CameraTransform cameraTransform;

    public SubLevelRenderSectionManager(final ClientSubLevel subLevel, final ClientLevel level, final int renderDistance, final CommandList commandList) {
        super(level, renderDistance, commandList);

        this.subLevel = subLevel;

        final OcclusionCullerExtension culler = (OcclusionCullerExtension) ((RenderSectionManagerExtension) this).sable$getOcclusionCuller();
        culler.sable$setSubLevel(subLevel);
    }

    public void apply(final ChunkRenderMatrices matrices, final double camX, final double camY, final double camZ) {
        final SubLevelRenderData renderer = this.subLevel.getRenderData();

        this.modelView.set(matrices.modelView());
        this.projection.set(RenderSystem.getProjectionMatrix());
        renderer.getChunkOffset(this.chunkOffset);

        final Vector3f pos = new Vector3f((float) camX, (float) camY, (float) camZ);
        renderer.getTransformation(0, 0, 0).invert().transformPosition(pos);
        this.cameraTransform = new CameraTransform(pos.x - this.chunkOffset.x, pos.y - this.chunkOffset.y, pos.z - this.chunkOffset.z);

    }


    public void render(final ChunkRenderMatrices originalMatrices, final RenderType layer, final double camX, final double camY, final double camZ) {
        final DefaultChunkRendererExtension chunkRenderer = (DefaultChunkRendererExtension) ((RenderSectionManagerExtension) this).sable$getChunkRenderer();
        chunkRenderer.sable$setCameraTransform(this.cameraTransform);

        final PoseStack matrixStack = new PoseStack();
        matrixStack.mulPose(new Matrix4f(originalMatrices.modelView()));
        matrixStack.pushPose();

        final Pose3dc pose = this.subLevel.renderPose();

        final Vector3dc spos = pose.position();
        final Vector3dc scale = pose.scale();
        final Quaterniondc orientation = pose.orientation();

        matrixStack.translate(spos.x() - camX, spos.y() - camY, spos.z() - camZ);
        matrixStack.mulPose(new Quaternionf(orientation));
        matrixStack.scale((float) scale.x(), (float) scale.y(), (float) scale.z());

        this.modelView.set(matrixStack.last().pose());
        matrixStack.popPose();
        final ChunkRenderMatrices matrices = new ChunkRenderMatrices(RenderSystem.getProjectionMatrix(), this.modelView);


        if (layer == RenderType.solid()) {
            this.renderLayer(matrices, DefaultTerrainRenderPasses.SOLID, -this.chunkOffset.x, -this.chunkOffset.y, -this.chunkOffset.z);
            this.renderLayer(matrices, DefaultTerrainRenderPasses.CUTOUT, -this.chunkOffset.x, -this.chunkOffset.y, -this.chunkOffset.z);
        } else if (layer == RenderType.translucent()) {
            this.renderLayer(matrices, DefaultTerrainRenderPasses.TRANSLUCENT, -this.chunkOffset.x, -this.chunkOffset.y, -this.chunkOffset.z);
        }

        chunkRenderer.sable$setCameraTransform(null);
    }


    /**
     * @return if occlusion culling should be disabled for this section manager
     */
    public boolean shouldDisableOcclusionCulling() {
        return true;
    }

}
