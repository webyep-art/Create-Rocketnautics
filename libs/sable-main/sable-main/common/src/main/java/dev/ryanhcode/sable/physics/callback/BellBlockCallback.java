package dev.ryanhcode.sable.physics.callback;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class BellBlockCallback extends FragileBlockCallback {
    public static final BellBlockCallback INSTANCE = new BellBlockCallback();

    public BellBlockCallback() {}

    @Override
    public boolean shouldTriggerFor(final BlockState state) {
        return state.getBlock() instanceof BellBlock;
    }

    @Override
    public CollisionResult onHit(final ServerLevel level, final BlockPos pos, final BlockState state, final Vector3d hitPos) {
        final Vec3 hitDir = pos.getCenter().subtract(hitPos.x, hitPos.y, hitPos.z);
        final Direction facing = state.getValue(BellBlock.FACING);
        final BellAttachType attachment = state.getValue(BellBlock.ATTACHMENT);

        int xMul = Math.abs(facing.getStepX());
        int zMul = Math.abs(facing.getStepZ());

        if (attachment == BellAttachType.CEILING) {
            xMul = 1;
            zMul = 1;
        }

        final Direction direction = Direction.getNearest(hitDir.x * xMul, 0.0, hitDir.z * zMul);
        ((BellBlock) state.getBlock()).attemptToRing(level, pos, direction.getOpposite());

        return new CollisionResult(JOMLConversion.ZERO, false);
    }
}
