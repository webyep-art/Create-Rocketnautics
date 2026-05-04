package dev.devce.rocketnautics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.orbit.DeepSpaceData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@EventBusSubscriber(modid = RocketNautics.MODID, value = Dist.CLIENT)
public class DeepSpaceHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !DeepSpaceData.isDeepSpace()) return;

        Camera camera = mc.gameRenderer.getMainCamera();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();
        matrix.identity();

        Quaternionf invRot = new Quaternionf(camera.rotation());
        invRot.conjugate();
        poseStack.mulPose(invRot);

        int renderDist = mc.options.renderDistance().get();
//        float parallaxFactor = (float) (renderDist / Math.max(100.0, ));
        // TODO
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (DeepSpaceData.isDeepSpace()) {
            event.setCanceled(true);
        }
    }
}
