package dev.devce.rocketnautics.content.blocks.parachute;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParachuteRenderer extends SafeBlockEntityRenderer<ParachuteCaseBlockEntity> {

    private static final int    CANOPY_R    = 2;
    private static final int    ROPE_SEGS   = 16;   // verlet segments per shroud line
    private static final float  GRAVITY     = -0.008f;
    private static final float  DAMPING     = 0.90f;
    private static final float  ROPE_THICK  = 0.04f; // visual thickness (blocks)

    public ParachuteRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(ParachuteCaseBlockEntity be, float partialTicks,
                               PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

        BlockState state = be.getBlockState();
        if (!state.hasProperty(ParachuteCaseBlock.OPEN) || !state.getValue(ParachuteCaseBlock.OPEN)) {
            be.resetPhysics();
            return;
        }
        if (!be.hasParachute()) return;

        // 1. Calculate animation frame smoothly and independent of frame rate
        long time = System.currentTimeMillis();
        if (be.lastRenderTime == 0) be.lastRenderTime = time;
        float dt = (time - be.lastRenderTime) / 50.0f; // roughly 20 ticks per second
        be.lastRenderTime = time;
        if (dt > 2.0f) dt = 2.0f; // cap to prevent jumps

        boolean falling = be.isFalling();
        if (dt > 0) {
            float targetY = falling ? 8.0f : 0.0f;
            float accel = (targetY - be.canopyY) * (falling ? 0.2f : 0.8f);
            if (!falling) accel -= 0.4f; // Smoother gravity pull when collapsing

            be.canopyVelocity += accel * dt;
            be.canopyVelocity *= (float) Math.pow(falling ? 0.7 : 0.6, dt);
            be.canopyY += be.canopyVelocity * dt;

            if (be.canopyY < 0.0f) { be.canopyY = 0.0f; be.canopyVelocity = 0.0f; }
            if (be.canopyY > 8.0f) { be.canopyY = 8.0f; be.canopyVelocity = 0.0f; }
        }

        // 2. Compute World-Up in Local Space to prevent "Arrow" effect
        // In modern Minecraft, the PoseStack inside a BlockEntityRenderer is already aligned
        // to the world axes (it does not contain the camera rotation, only the Ship's rotation).
        Matrix4f pose = ms.last().pose();
        Matrix3f shipRotMat = new Matrix3f();
        pose.get3x3(shipRotMat); // This is exactly the ship's rotation!
        
        Matrix3f invShipRot = new Matrix3f(shipRotMat).invert(); // From World to Local
        
        // The World UP vector (0, 1, 0) transformed into the Block's local space
        Vector3f localUp = new Vector3f(0, 1, 0);
        invShipRot.transform(localUp);
        localUp.normalize();

        // 3. Canopy center in local space (starts at 0.5, 1.0, 0.5, then goes UP in the world)
        Vector3f canopyCenterLocal = new Vector3f(0.5f, 1.0f, 0.5f)
                .add(localUp.x() * be.canopyY, localUp.y() * be.canopyY, localUp.z() * be.canopyY);

        // 4. Compute 4 rope attachment corners in local space
        int[][] CORNERS = { { -2,  2 }, {  2,  2 }, { -2, -2 }, {  2, -2 } };
        Vector3f[] cornersLocal = new Vector3f[4];
        for (int r = 0; r < 4; r++) {
            Vector3f worldOffset = new Vector3f(CORNERS[r][0], 0, CORNERS[r][1]);
            Vector3f localOffset = new Vector3f();
            invShipRot.transform(worldOffset, localOffset);
            cornersLocal[r] = new Vector3f(canopyCenterLocal).add(localOffset);
        }

        // 5. Render Canopy horizontally in the world
        ms.pushPose();
        // Translate to the canopy center
        ms.translate(canopyCenterLocal.x(), canopyCenterLocal.y(), canopyCenterLocal.z());
        // Apply inverse ship rotation so the canopy renders aligned to the world axes
        Quaternionf invShipQuat = new Quaternionf().setFromNormalized(invShipRot);
        ms.mulPose(invShipQuat);
        
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
        BlockState wool = Blocks.WHITE_WOOL.defaultBlockState();

        // Dome Layer 0 (5x5)
        for (int x = -CANOPY_R; x <= CANOPY_R; x++) {
            for (int z = -CANOPY_R; z <= CANOPY_R; z++) {
                renderBlock(ms, buffer, brd, wool, x, 0, z, light, overlay);
            }
        }
        // Dome Layer 1 (3x3 top)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                renderBlock(ms, buffer, brd, wool, x, 1.0f, z, light, overlay);
            }
        }
        ms.popPose();

        // 6. Physics and ropes using dynamically calculated corner locations
        float[][][] pts = be.getOrInitPhysicsPoints(cornersLocal, ROPE_SEGS);
        be.stepPhysics(GRAVITY, DAMPING, cornersLocal, ROPE_SEGS);

        for (int rope = 0; rope < 4; rope++) {
            for (int seg = 0; seg < ROPE_SEGS; seg++) {
                float x1 = pts[rope][seg][0],   y1 = pts[rope][seg][1],   z1 = pts[rope][seg][2];
                float x2 = pts[rope][seg+1][0], y2 = pts[rope][seg+1][1], z2 = pts[rope][seg+1][2];
                renderRopeSegment(ms, buffer, x1, y1, z1, x2, y2, z2, light, overlay);
            }
        }
    }

    private static final ResourceLocation ROPE_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/brown_wool.png");

    private static void renderRopeSegment(PoseStack ms, MultiBufferSource buffer,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           int light, int overlay) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-4f) return;

        ms.pushPose();
        ms.translate(x1 + dx * 0.5f, y1 + dy * 0.5f, z1 + dz * 0.5f);

        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f dir = new Vector3f(dx, dy, dz).normalize();
        Quaternionf rot = new Quaternionf().rotationTo(up, dir);
        ms.mulPose(rot);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(ROPE_TEX));
        PoseStack.Pose pose = ms.last();
        
        float w = 0.10f; // very thick rope width
        float h = len * 0.5f;
        
        // Map a narrow strip of the brown wool texture to prevent aliasing
        float u1 = 0.4f, u2 = 0.6f;
        float v1 = 0.0f, v2 = len;

        // Plane 1 (XY)
        addVertex(vc, pose, -w, -h,  0, u1, v2, light, overlay);
        addVertex(vc, pose,  w, -h,  0, u2, v2, light, overlay);
        addVertex(vc, pose,  w,  h,  0, u2, v1, light, overlay);
        addVertex(vc, pose, -w,  h,  0, u1, v1, light, overlay);

        // Plane 1 back
        addVertex(vc, pose, -w,  h,  0, u1, v1, light, overlay);
        addVertex(vc, pose,  w,  h,  0, u2, v1, light, overlay);
        addVertex(vc, pose,  w, -h,  0, u2, v2, light, overlay);
        addVertex(vc, pose, -w, -h,  0, u1, v2, light, overlay);

        // Plane 2 (YZ)
        addVertex(vc, pose, 0, -h, -w, u1, v2, light, overlay);
        addVertex(vc, pose, 0, -h,  w, u2, v2, light, overlay);
        addVertex(vc, pose, 0,  h,  w, u2, v1, light, overlay);
        addVertex(vc, pose, 0,  h, -w, u1, v1, light, overlay);

        // Plane 2 back
        addVertex(vc, pose, 0,  h, -w, u1, v1, light, overlay);
        addVertex(vc, pose, 0,  h,  w, u2, v1, light, overlay);
        addVertex(vc, pose, 0, -h,  w, u2, v2, light, overlay);
        addVertex(vc, pose, 0, -h, -w, u1, v2, light, overlay);

        ms.popPose();
    }

    private static void addVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, int overlay) {
        vc.addVertex(pose.pose(), x, y, z)
          .setColor(1.0f, 1.0f, 1.0f, 1.0f)
          .setUv(u, v)
          .setOverlay(overlay)
          .setLight(light)
          .setNormal(pose, 0, 1, 0);
    }

    private static void renderBlock(PoseStack ms, MultiBufferSource buffer,
                                     BlockRenderDispatcher brd, BlockState state,
                                     float x, float y, float z, int light, int overlay) {
        ms.pushPose();
        // Since canopyCenterLocal already placed us at the precise center of a block (0.5), 
        // we subtract 0.5f so the block's true center visually lands on our integer grid points
        ms.translate(x - 0.5f, y, z - 0.5f);
        brd.renderSingleBlock(state, ms, buffer, light, overlay);
        ms.popPose();
    }
}
