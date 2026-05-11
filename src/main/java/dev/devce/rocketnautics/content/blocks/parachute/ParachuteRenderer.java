package dev.devce.rocketnautics.content.blocks.parachute;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.devce.rocketnautics.registry.RocketPartials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class ParachuteRenderer extends SafeBlockEntityRenderer<ParachuteCaseBlockEntity> {

    public ParachuteRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ParachuteCaseBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (state.hasProperty(ParachuteCaseBlock.OPEN) && state.getValue(ParachuteCaseBlock.OPEN)) {
            net.minecraft.core.Direction facing = state.getValue(ParachuteCaseBlock.FACING);
            
            ms.pushPose();
            
            // Center in block
            ms.translate(0.5, 0.5, 0.5);
            
            // Rotate based on facing
            if (facing == net.minecraft.core.Direction.DOWN) {
                ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
            } else if (facing != net.minecraft.core.Direction.UP) {
                // First rotate to the horizon (XP 90), then to the correct compass direction (YP)
                float yRot = facing.toYRot();
                ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yRot));
                ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
            }

            // Scale and then translate to center the 1x1 model base at the block center
            ms.scale(4.0f, 4.0f, 4.0f);
            ms.translate(-0.5, 0.1, -0.5); 


            if (RocketPartials.openParachute != null) {
                Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                        ms.last(), buffer.getBuffer(RenderType.cutout()), state, RocketPartials.openParachute, 
                        1.0f, 1.0f, 1.0f, light, overlay);
            }

            ms.popPose();
        }
    }
}
