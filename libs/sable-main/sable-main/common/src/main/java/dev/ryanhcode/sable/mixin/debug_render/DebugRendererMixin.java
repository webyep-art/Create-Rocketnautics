package dev.ryanhcode.sable.mixin.debug_render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {

    /**
     * @author RyanH
     * @reason Take into account sub-levels
     */
    @Overwrite
    public static void renderFilledBox(final PoseStack poseStack, final MultiBufferSource bufferSource, final BlockPos blockPos, final float f, final float g, final float h, final float i, final float j) {
        final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isInitialized()) {
            final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(blockPos);

            if (subLevel != null) {
                poseStack.pushPose();
                final Pose3dc renderPose = subLevel.renderPose();
                final Vec3 pos = renderPose.transformPosition(blockPos.getCenter()).subtract(camera.getPosition());
                poseStack.translate(pos.x, pos.y, pos.z);
                poseStack.mulPose(new Quaternionf(renderPose.orientation()));
                DebugRenderer.renderFilledBox(poseStack, bufferSource, new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0).inflate(0.5).inflate(f), g, h, i, j);
                poseStack.popPose();
                return;
            }

            final Vec3 relativePos = camera.getPosition().reverse();
            final AABB box = new AABB(blockPos).move(relativePos).inflate(f);


            DebugRenderer.renderFilledBox(poseStack, bufferSource, box, g, h, i, j);
        }
    }

}
