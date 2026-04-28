package dev.devce.rocketnautics.content.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * RocketThrusterRenderer — Cassini-style plasma plume.
 *
 * Target look (from Cassini mod reference):
 *  - Blinding white square bloom at nozzle exit
 *  - Short (5-6 block) cylindrical beam
 *  - Color: white core → light-blue → purple/violet outer halo
 *  - Soft Mach disk pulsations, NOT sharp diamonds
 *  - Heavy soft bloom around the entire beam
 */
public class RocketThrusterRenderer extends SafeBlockEntityRenderer<AbstractThrusterBlockEntity> {

    public static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public RocketThrusterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(AbstractThrusterBlockEntity be, float partialTicks,
                              PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!be.isActive()) return;

        float power = be.getRenderPower();
        Direction facing = be.getThrustDirection();

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        applyOrientation(ms, facing);
        renderCassiniPlume(be.getLevel(), partialTicks, ms, buffer, power);
        ms.popPose();
    }

    public static void applyOrientation(PoseStack ms, Direction facing) {
        switch (facing) {
            case DOWN  -> ms.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> ms.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> ms.mulPose(Axis.XP.rotationDegrees(90));
            case WEST  -> ms.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST  -> ms.mulPose(Axis.ZP.rotationDegrees(-90));
            default    -> {}
        }
    }

    // =========================================================================
    //  MAIN CASSINI PLUME
    // =========================================================================

    public static void renderCassiniPlume(net.minecraft.world.level.Level level, float partialTicks,
                                    PoseStack ms, MultiBufferSource buffer, float power) {
        long time = level == null ? 0 : level.getGameTime();
        float t = time + partialTicks;

        // 1. NOZZLE EXIT BLOOM (soft cross, no ugly squares)
        renderNozzleBloom(ms, buffer, power, t);

        ms.translate(0, 0.4375, 0);

        // Length scales with thrust power — at 100% throttle: full length; at 0%: nothing
        float lengthScale = power;

        // 2. OUTER VIOLET ATMOSPHERIC HALO
        renderCylinderLayer(ms, buffer, 28.0f * lengthScale, 1.05f * power, 0x7040FF, 0x30108A,
                0.22f, 0.00f, t * 0.06f, t, 0.0f, 0);
        renderCylinderLayer(ms, buffer, 25.0f * lengthScale, 0.825f * power, 0x9060FF, 0x5020BB,
                0.30f, 0.00f, t * 0.10f, t, 0.0f, 30);

        // 3. MID PURPLE-BLUE SHEATH
        renderCylinderLayer(ms, buffer, 22.0f * lengthScale, 0.5625f * power, 0xC090FF, 0x7040CC,
                0.50f, 0.02f, t * 0.20f, t, 0.02f, 15);

        // 4. INNER BRIGHT CORE
        renderCylinderLayer(ms, buffer, 19.0f * lengthScale, 0.3375f * power, 0xFFFFFF, 0xC0B0FF,
                0.75f, 0.05f, t * 0.60f, t, 0.03f, 45);

        // 5. NUCLEAR WHITE NEEDLE
        float flicker = 0.90f + 0.10f * Mth.sin(t * 4.2f);
        renderCylinderLayer(ms, buffer, 16.0f * lengthScale, 0.15f * power, 0xFFFFFF, 0xFFFFFF,
                1.00f * flicker, 0.10f, t * 2.0f, t, 0.0f, 0);

        // 6. MACH SHOCK DIAMONDS — length also scales with power
        renderMachShockDiamonds(ms, buffer, power, t, lengthScale);
    }

    /**
     * Renders bright inner diamond shapes at each Mach disk pinch point.
     * In real rocket engines, the shock structure creates triangular/diamond
     * bright spots at the narrowings of the plume.
     */
    public static void renderMachShockDiamonds(PoseStack ms, MultiBufferSource buffer, float power, float t, float lengthScale) {
        VertexConsumer vc = buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
        float period = 3.5f;
        float beamLen = 16.0f * lengthScale;

        ms.pushPose();

        int diamondCount = (int)(beamLen / period);
        for (int i = 0; i < diamondCount; i++) {
            float y = period * i + period * 0.75f;

            // Steep fade — first diamond ~1.0, last almost invisible
            float distFade = (float) Math.pow(1.0 - (y / beamLen), 3.0);
            if (distFade <= 0.01f) continue;

            float diamondRadius = 0.28f * power * distFade;  // Bigger
            float diamondHeight = period * 0.5f;

            float dFlicker = 0.88f + 0.12f * Mth.sin(t * 5.0f + i * 1.7f);
            float alphaPeak = distFade * dFlicker;  // Full brightness at first, fades fast

            renderDiamondHalf(ms, vc, y - diamondHeight * 0.5f, y,
                    0.0f, diamondRadius, 0xFFFFFF, 0xFFE8FF, alphaPeak, alphaPeak * 0.5f);
            renderDiamondHalf(ms, vc, y, y + diamondHeight * 0.5f,
                    diamondRadius, 0.0f, 0xFFE8FF, 0xFFFFFF, alphaPeak * 0.5f, alphaPeak);
        }

        ms.popPose();
    }

    public static void renderDiamondHalf(PoseStack ms, VertexConsumer vc,
                                    float y0, float y1, float r0, float r1,
                                    int c0, int c1, float a0, float a1) {
        // 4-sided (diamond shape)
        renderRing(ms, vc, y0, y1, r0, r1, c0, c1, a0, a1, 0, 0, 4);
    }

    // =========================================================================
    //  NOZZLE BLOOM — blinding white square at the exit (key Cassini feature)
    // =========================================================================

    public static void renderNozzleBloom(PoseStack ms, MultiBufferSource buffer, float power, float t) {
        VertexConsumer vc = buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
        ms.pushPose();
        ms.translate(0, 0.4375, 0);

        // Only the tight white core disc — no squares
        renderQuad(ms, vc, 0.3125f * power, 0xFFFFFF, 1.0f);
        float pulse = 1.0f + 0.06f * Mth.sin(t * 2.0f);
        renderQuad(ms, vc, 0.45f  * power * pulse, 0xFFFFFF, 0.60f);
        ms.mulPose(Axis.YP.rotationDegrees(45));
        renderQuad(ms, vc, 0.45f  * power * pulse, 0xFFFFFF, 0.60f);

        ms.popPose();
    }


    // =========================================================================
    //  CYLINDER LAYER RENDERER (smooth, round — like Cassini, NOT pyramid)
    // =========================================================================

    /**
     * @param height       total length of the cylinder in blocks
     * @param baseRadius   radius at the widest Mach disk point
     * @param colorNear    colour near the nozzle (hex RGB)
     * @param colorFar     colour at the far end
     * @param alphaNear    opacity at the nozzle end
     * @param alphaFar     opacity at the far end (can be 0 for full fade)
     * @param vScroll      UV scrolling offset (for animation feel)
     * @param t            game time for jitter
     * @param jitter       radius jitter amplitude
     * @param rotDeg       static rotation in degrees
     */
    public static void renderCylinderLayer(PoseStack ms, MultiBufferSource buffer,
                                     float height, float baseRadius,
                                     int colorNear, int colorFar,
                                     float alphaNear, float alphaFar,
                                     float vScroll, float t,
                                     float jitter, float rotDeg) {
        VertexConsumer vc = buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
        ms.pushPose();
        ms.mulPose(Axis.YP.rotationDegrees(rotDeg));

        final int SIDES    = 24;  // Smooth circular Mach rings
        final int SEGMENTS = (int)(height * 8);  // Dense enough for smooth taper
        float segH = height / SEGMENTS;

        for (int i = 0; i < SEGMENTS; i++) {
            float y0 = i       * segH;
            float y1 = (i + 1) * segH;
            float p0 = y0 / height;
            float p1 = y1 / height;

            // Gaussian alpha falloff — the key to Cassini's soft look
            float a0 = Mth.lerp(p0, alphaNear, alphaFar) * gaussianFade(p0);
            float a1 = Mth.lerp(p1, alphaNear, alphaFar) * gaussianFade(p1);

            int c0 = lerpColor(colorNear, colorFar, p0);
            int c1 = lerpColor(colorNear, colorFar, p1);

            float r0 = machRadius(y0, baseRadius, t, jitter, height);
            float r1 = machRadius(y1, baseRadius, t, jitter, height);

            renderRing(ms, vc, y0, y1, r0, r1, c0, c1, a0, a1,
                       -y0 * 0.5f + vScroll, -y1 * 0.5f + vScroll, SIDES);
        }

        ms.popPose();
    }

    /**
     * Gaussian (soft) fade — power-3 gives Cassini's characteristic "melting" edge.
     */
    private static float gaussianFade(float p) {
        return (float) Math.pow(1.0 - p, 2.5);
    }

    /**
     * Mach disk radius function.
     * Uses a gentle sine wave (NOT triangle wave) for soft pulsations like Cassini.
     * Initial underexpansion: beam is narrower at the nozzle and expands quickly.
     */
    private static float machRadius(float y, float base, float t, float jitter, float height) {
        // Underexpansion: starts at ~60% width at nozzle, reaches 100% within ~0.5 blocks
        float underexpand = 0.60f + 0.40f * Mth.clamp(y / 0.5f, 0, 1);

        // Soft Mach disk pulsation — sine, NOT triangle wave
        float period = 3.5f;
        float machPulse = 0.10f * Mth.sin((float)(y / period * Math.PI * 2));

        // Gentle taper toward the end
        float taper = (float) Math.pow(1.0 - (y / height), 0.6);

        float micro = jitter * Mth.sin(t * 18f + y * 40f);

        return base * underexpand * (1.0f + machPulse) * taper + micro;
    }

    // =========================================================================
    //  GEOMETRY PRIMITIVES
    // =========================================================================



    public static void renderRing(PoseStack ms, VertexConsumer vc,
                             float y0, float y1, float r0, float r1,
                             int c0, int c1, float a0, float a1,
                             float v0, float v1, int sides) {
        PoseStack.Pose pose = ms.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        float rr0 = (c0 >> 16 & 255) / 255f, rg0 = (c0 >> 8 & 255) / 255f, rb0 = (c0 & 255) / 255f;
        float rr1 = (c1 >> 16 & 255) / 255f, rg1 = (c1 >> 8 & 255) / 255f, rb1 = (c1 & 255) / 255f;

        Vector3f nrm = new Vector3f(0, 1, 0).mul(n);
        float nx = nrm.x(), ny = nrm.y(), nz = nrm.z();

        for (int i = 0; i < sides; i++) {
            float a  = (float)(i       * 2 * Math.PI / sides);
            float b  = (float)((i + 1) * 2 * Math.PI / sides);
            float u0 = (float) i       / sides;
            float u1 = (float)(i + 1)  / sides;

            float ca = Mth.cos(a), sa = Mth.sin(a);
            float cb = Mth.cos(b), sb = Mth.sin(b);

            vc.addVertex(m, ca*r0, y0, sa*r0).setColor(rr0,rg0,rb0,a0).setUv(u0,v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
            vc.addVertex(m, ca*r1, y1, sa*r1).setColor(rr1,rg1,rb1,a1).setUv(u0,v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
            vc.addVertex(m, cb*r1, y1, sb*r1).setColor(rr1,rg1,rb1,a1).setUv(u1,v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
            vc.addVertex(m, cb*r0, y0, sb*r0).setColor(rr0,rg0,rb0,a0).setUv(u1,v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
        }
    }

    public static void renderQuad(PoseStack ms, VertexConsumer vc, float size, int color, float alpha) {
        PoseStack.Pose pose = ms.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        float r = (color >> 16 & 255) / 255f;
        float g = (color >>  8 & 255) / 255f;
        float b = (color       & 255) / 255f;

        Vector3f nrm = new Vector3f(0, 1, 0).mul(n);
        float nx = nrm.x(), ny = nrm.y(), nz = nrm.z();

        vc.addVertex(m, -size, 0, -size).setColor(r,g,b,alpha).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
        vc.addVertex(m, -size, 0,  size).setColor(r,g,b,alpha).setUv(0,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
        vc.addVertex(m,  size, 0,  size).setColor(r,g,b,alpha).setUv(1,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
        vc.addVertex(m,  size, 0, -size).setColor(r,g,b,alpha).setUv(1,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx,ny,nz);
    }

    // =========================================================================
    //  UTILITIES
    // =========================================================================

    public static int lerpColor(int c0, int c1, float t) {
        int r = (int) Mth.lerp(t, c0 >> 16 & 255, c1 >> 16 & 255);
        int g = (int) Mth.lerp(t, c0 >>  8 & 255, c1 >>  8 & 255);
        int b = (int) Mth.lerp(t, c0       & 255, c1       & 255);
        return r << 16 | g << 8 | b;
    }
}
