package dev.devce.rocketnautics.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.SkyDataHandler;
import dev.devce.rocketnautics.content.orbit.DeepSpaceData;
import dev.devce.rocketnautics.content.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.content.orbit.universe.CubePlanet;
import dev.devce.rocketnautics.content.orbit.universe.DeepSpacePosition;
import dev.devce.rocketnautics.content.orbit.universe.UniverseDefinition;
import dev.devce.rocketnautics.network.PlanetRenderRequestPayload;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.orekit.time.AbsoluteDate;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = RocketNautics.MODID, value = Dist.CLIENT)
public final class DeepSpaceHandler {

    private static @Nullable UniverseDefinition UNIVERSE;
    private static final Int2ObjectAVLTreeMap<IntObjectPair<byte[]>> KNOWN_RENDER_DATA = new Int2ObjectAVLTreeMap<>();

    private static long receivedPositionTick = -1;
    private static final DeepSpacePosition receivedPosition = new DeepSpacePosition();

    public static void receiveUniverse(UniverseDefinition definition) {
        UNIVERSE = definition;
    }

    public static void receivePosition(FriendlyByteBuf buf) {
        if (UNIVERSE != null) {
            receivedPositionTick = Minecraft.getInstance().levelRenderer.getTicks();
            receivedPosition.read(buf, UNIVERSE);
        }
    }

    public static void receiveRenderData(int id, byte[] data, int powerScale) {
        KNOWN_RENDER_DATA.put(id, IntObjectPair.of(powerScale, data));
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || UNIVERSE == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!DeepSpaceData.isDeepSpace() && !KNOWN_RENDER_DATA.isEmpty()) {
            KNOWN_RENDER_DATA.clear();
            return;
        }
        if (receivedPositionTick == -1) return;
        Camera camera = mc.gameRenderer.getMainCamera();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();
        matrix.identity();

        Quaternionf invRot = new Quaternionf(camera.rotation());
        invRot.conjugate();
        poseStack.mulPose(invRot);

        renderStars(event.getLevelRenderer(), poseStack, event.getProjectionMatrix());

        int renderDist = mc.options.renderDistance().get();
        IntList needRenderData = new IntArrayList();
        AbsoluteDate currentDate = receivedPosition.getLocalUniverseTime().shiftedBy(receivedPosition.getTimescale() * (event.getPartialTick().getGameTimeDeltaPartialTick(true) + event.getRenderTick() - receivedPositionTick) / 20);
        Vector3D posInFrame = receivedPosition.getPVCoords(currentDate).getPosition();
        Iterator<Pair<Vector3D, CubePlanet>> iter = UNIVERSE.getPlanets().stream().map(planet -> {
            Vector3D ourPosInPlanetFrame = receivedPosition.getFrame().getStaticTransformTo(planet.orekitFrame(), currentDate).transformPosition(posInFrame);
            return Pair.of(ourPosInPlanetFrame, planet);
        }).sorted(Comparator.comparingDouble(p -> -p.left().getNormSq())).iterator(); // sort descending, we want to render furthest away first.
        while (iter.hasNext()) {
            Pair<Vector3D, CubePlanet> planet = iter.next();
            poseStack.pushPose();
            if (renderPlanet(planet.right(), planet.left(), poseStack, posInFrame, currentDate, renderDist, 1)) {
                needRenderData.add(planet.right().id());
            }
            poseStack.popPose();
        }
        PacketDistributor.sendToServer(new PlanetRenderRequestPayload(needRenderData.toIntArray(), SkyHandler.getMaximumScale()));
        poseStack.popPose();
    }

    private static boolean renderPlanet(CubePlanet planet, Vector3D ourPosInPlanetFrame, PoseStack poseStack, Vector3D posInFrame, AbsoluteDate date, float renderDist, float visibility) {
        IntObjectPair<byte[]> render = KNOWN_RENDER_DATA.get(planet.id());
        if (render == null || render.leftInt() != SkyHandler.getMaximumScale() || render.right() == null) {
            return true;
        }
        float parallaxFactor = (float) (renderDist / Math.max(1, ourPosInPlanetFrame.getNorm()));
        poseStack.translate(-ourPosInPlanetFrame.getX() * parallaxFactor, -ourPosInPlanetFrame.getY() * parallaxFactor, -ourPosInPlanetFrame.getZ() * parallaxFactor);
        poseStack.pushPose();
        poseStack.mulPose(DeepSpaceHelper.adapt(planet.getRotationAtTime(date)).get(new Quaternionf()));
        float size = (float) (planet.radius() * parallaxFactor);

        DeepSpaceTexture.getInstance().loadData(render.right());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, DeepSpaceTexture.getInstanceID());

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float r = 1.0f, g = 1.0f, b = 1.0f;

        Matrix4f matrix = poseStack.last().pose();

        // TOP face - CCW from above
        bufferbuilder.addVertex(matrix, -size, size, -size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);
        bufferbuilder.addVertex(matrix, -size, size, size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);
        bufferbuilder.addVertex(matrix, size, size, size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, size, size, -size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);

        // BOTTOM - CCW from below
        bufferbuilder.addVertex(matrix, -size, -size, -size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);
        bufferbuilder.addVertex(matrix, size, -size, -size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);
        bufferbuilder.addVertex(matrix, size, -size, size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, -size, -size, size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);

        // NORTH (Z = -size) - CCW from North
        bufferbuilder.addVertex(matrix, size, -size, -size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);
        bufferbuilder.addVertex(matrix, -size, -size, -size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, -size, size, -size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);
        bufferbuilder.addVertex(matrix, size, size, -size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);

        // SOUTH (Z = size) - CCW from South
        bufferbuilder.addVertex(matrix, -size, -size, size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);
        bufferbuilder.addVertex(matrix, size, -size, size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, size, size, size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);
        bufferbuilder.addVertex(matrix, -size, size, size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);

        // WEST (X = -size) - CCW from West
        bufferbuilder.addVertex(matrix, -size, size, size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);
        bufferbuilder.addVertex(matrix, -size, size, -size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);
        bufferbuilder.addVertex(matrix, -size, -size, -size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, -size, -size, size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);

        // EAST (X = size) - CCW from East
        bufferbuilder.addVertex(matrix, size, size, -size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);
        bufferbuilder.addVertex(matrix, size, size, size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);
        bufferbuilder.addVertex(matrix, size, -size, size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, size, -size, -size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);

        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

        // TODO clouds

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        int layers = 20; // TODO make the number of layers a config option
        float ar = 0.2f, ag = 0.5f, ab = 1.0f;

        for (int i = 0; i < layers; i++) {
            float progress = i / (float) (layers - 1);
            // Progressive size from 1.01 to 1.4 (tighter range)
            float s = size * (1.01f + (float)Math.pow(progress, 1.2f) * 0.4f);
            // Adjusted alpha falloff for tighter layers
            float aa = (0.05f * (float)Math.pow(1.0f - progress, 2.0f)) * visibility;

            BufferBuilder atmBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // Render all 6 faces using matrix
            atmBuilder.addVertex(matrix, -s, s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, -s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, -s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, -s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, -s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, -s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, -s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, -s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, -s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, -s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, -s, -s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, s, -s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, -s, s).setColor(ar, ag, ab, aa);
            atmBuilder.addVertex(matrix, s, -s, -s).setColor(ar, ag, ab, aa);

            BufferUploader.drawWithShader(atmBuilder.buildOrThrow());
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        poseStack.popPose();
        return false;
    }

    private static void renderStars(LevelRenderer renderer, PoseStack poseStack, Matrix4f projectionMatrix) {
        VertexBuffer starBuffer = ((StarBufferExposer) renderer).rocketnautics$starBuffer();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        FogRenderer.setupNoFog();
        starBuffer.bind();
        starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
    }
}
