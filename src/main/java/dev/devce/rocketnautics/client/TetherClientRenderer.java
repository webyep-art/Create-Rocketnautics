package dev.devce.rocketnautics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.event.RopeHandler;
import dev.devce.rocketnautics.registry.RocketItems;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class TetherClientRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(RocketItems.SPACE_CHESTPLATE.get())) {
            return;
        }

        Vec3 anchor = RopeHandler.getProjectedAnchor(mc.player);
        if (anchor == null) {
            return;
        }

        Vec3 chest = mc.player.getEyePosition().add(0.0, -0.35, 0.0);
        chest = SableCompanion.INSTANCE.projectOutOfSubLevel(mc.level, chest);

        if (chest.distanceToSqr(anchor) < 0.0001) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        int segments = Mth.clamp((int) (Math.sqrt(chest.distanceToSqr(anchor)) * 3.0), 8, 32);
        Vec3 previous = curvePoint(chest, anchor, 0.0);

        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 current = curvePoint(chest, anchor, t);
            float shade = 1.0f - (float) t * 0.15f;

            builder.addVertex(matrix, (float) previous.x, (float) previous.y, (float) previous.z)
                    .setColor(0.86f * shade, 0.86f * shade, 0.90f * shade, 1.0f);
            builder.addVertex(matrix, (float) current.x, (float) current.y, (float) current.z)
                    .setColor(0.74f * shade, 0.74f * shade, 0.78f * shade, 1.0f);

            previous = current;
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static Vec3 curvePoint(Vec3 start, Vec3 end, double t) {
        Vec3 point = start.lerp(end, t);
        double distance = start.distanceTo(end);
        double sag = Math.sin(Math.PI * t) * Math.min(0.20 + distance * 0.02, 1.10);
        return point.add(0.0, -sag, 0.0);
    }
}
