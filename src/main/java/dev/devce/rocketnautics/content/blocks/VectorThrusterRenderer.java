package dev.devce.rocketnautics.content.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.devce.rocketnautics.registry.RocketPartials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class VectorThrusterRenderer extends SafeBlockEntityRenderer<VectorThrusterBlockEntity> {

    public VectorThrusterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(VectorThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(RocketThrusterBlock.FACING);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);

        float gX = be.getRenderGimbalX();
        float gY = be.getRenderGimbalY();
        float gZ = be.getRenderGimbalZ();
        
        Vector3f dir = new Vector3f(facing.getStepX() - gX, facing.getStepY() - gY, facing.getStepZ() - gZ).normalize();
        
        int rotX = 0;
        int rotY = 0;
        switch (facing) {
            case DOWN -> rotX = 180;
            case EAST -> { rotX = 90; rotY = 90; }
            case NORTH -> rotX = 90;
            case SOUTH -> { rotX = 90; rotY = 180; }
            case WEST -> { rotX = 90; rotY = 270; }
            case UP -> {}
        }

        Vector3f localDir = new Vector3f(dir);
        localDir.rotateY((float) Math.toRadians(-rotY));
        localDir.rotateX((float) Math.toRadians(-rotX));

        Quaternionf qLocal = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), localDir);
        
        ms.mulPose(Axis.YP.rotationDegrees(rotY));
        ms.mulPose(Axis.XP.rotationDegrees(rotX));
        ms.mulPose(qLocal);
        
        ms.translate(-0.5, -0.5, -0.5);

        var bakedModel = RocketPartials.vectorThrusterNozzle;
        if (bakedModel != null) {
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    ms.last(), buffer.getBuffer(RenderType.cutout()), blockState, bakedModel, 
                    1.0f, 1.0f, 1.0f, light, overlay);
        }

        ms.popPose();
    }
}
