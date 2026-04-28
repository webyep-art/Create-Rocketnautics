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

/**
 * Renderer for the Vector Thruster gimbaled nozzle.
 */
public class VectorThrusterRenderer extends SafeBlockEntityRenderer<VectorThrusterBlockEntity> {

    public VectorThrusterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(VectorThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = be.getBlockState();
        Direction facing = be.getThrustDirection();

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);

        applyGimbalRotation(be, facing, ms);
        
        ms.translate(-0.5, -0.5, -0.5);

        renderNozzle(ms, buffer, light, overlay, blockState);

        if (be.isActive()) {
            ms.translate(0.5, 0.5, 0.5);
            RocketThrusterRenderer.renderCassiniPlume(be.getLevel(), partialTicks, ms, buffer, be.getRenderPower());
        }

        ms.popPose();
    }

    private void applyGimbalRotation(VectorThrusterBlockEntity be, Direction facing, PoseStack ms) {
        float gX = be.getRenderGimbalX();
        float gY = be.getRenderGimbalY();
        float gZ = be.getRenderGimbalZ();
        
        // Calculate the direction vector based on facing and gimbal angles
        Vector3f dir = new Vector3f(
                facing.getStepX() - gX, 
                facing.getStepY() - gY, 
                facing.getStepZ() - gZ
        ).normalize();
        
        // Get base rotation degrees for the block's facing
        float rotX = getBaseRotationX(facing);
        float rotY = getBaseRotationY(facing);

        // Transform the global direction vector into the block's local space
        Vector3f localDir = new Vector3f(dir);
        localDir.rotateY((float) Math.toRadians(-rotY));
        localDir.rotateX((float) Math.toRadians(-rotX));

        // Calculate rotation from base UP (0,1,0) to the local target direction
        Quaternionf gimbalRotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), localDir);
        
        // Apply transformations: Base facing -> Local gimbal rotation
        ms.mulPose(Axis.YP.rotationDegrees(rotY));
        ms.mulPose(Axis.XP.rotationDegrees(rotX));
        ms.mulPose(gimbalRotation);
    }

    private float getBaseRotationX(Direction facing) {
        return switch (facing) {
            case DOWN -> 180;
            case UP -> 0;
            default -> 90;
        };
    }

    private float getBaseRotationY(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    private void renderNozzle(PoseStack ms, MultiBufferSource buffer, int light, int overlay, BlockState state) {
        var bakedModel = RocketPartials.vectorThrusterNozzle;
        if (bakedModel != null) {
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    ms.last(), 
                    buffer.getBuffer(RenderType.cutout()), 
                    state, 
                    bakedModel, 
                    1.0f, 1.0f, 1.0f, 
                    light, 
                    overlay
            );
        }
    }
}
