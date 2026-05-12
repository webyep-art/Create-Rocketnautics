package dev.devce.rocketnautics.content.blocks.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Renderer for MultiRopeHubBlockEntity.
 * Rope strand visuals are drawn globally by Simulated's ClientLevelRopeManager / RopeStrandRenderer.
 * This renderer exists so the block entity type has a registered renderer context,
 * which Simulated requires to track and send rope strand data to the client.
 */
public class MultiRopeHubRenderer extends SafeBlockEntityRenderer<MultiRopeHubBlockEntity> {

    public MultiRopeHubRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(MultiRopeHubBlockEntity be, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Rope strand rendering is handled globally by Simulated's RopeStrandRenderer.
        // No additional per-block rendering needed here.
    }
}
