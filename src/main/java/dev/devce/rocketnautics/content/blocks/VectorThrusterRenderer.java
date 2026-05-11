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
import net.minecraft.util.Mth;
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

        
        float gX = Mth.lerp(partialTicks, be.getPrevGimbalX(), be.getGimbalX());
        float gY = Mth.lerp(partialTicks, be.getPrevGimbalY(), be.getGimbalY());
        float gZ = Mth.lerp(partialTicks, be.getPrevGimbalZ(), be.getGimbalZ());
        
        // Convert facing direction to a base rotation quaternion
        Quaternionf q = facing.getRotation();
        
        // Apply gimbal rotations locally. 
        // A deviation in X (gX) requires a rotation around the Z axis.
        // A deviation in Z (gZ) requires a rotation around the X axis.
        // We use Math.asin to roughly convert the linear deviation into an angle.
        if (gZ != 0) q.rotateX((float) -Math.asin(Mth.clamp(gZ, -1, 1)));
        if (gX != 0) q.rotateZ((float) Math.asin(Mth.clamp(gX, -1, 1)));
        if (gY != 0) q.rotateY((float) Math.asin(Mth.clamp(gY, -1, 1)));
        
        ms.mulPose(q);
        
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
