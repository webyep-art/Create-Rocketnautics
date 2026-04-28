package dev.ryanhcode.sable.neoforge.physics.callback;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public class BeltBlockCallback implements BlockSubLevelCollisionCallback {
    public static BeltBlockCallback INSTANCE = new BeltBlockCallback();

    private BeltBlockCallback() {}

    @Override
    public BlockSubLevelCollisionCallback.CollisionResult sable$onCollision(final BlockPos pos, final Vector3d pos1, final double impactVelocity) {
        final SubLevelPhysicsSystem system = SubLevelPhysicsSystem.getCurrentlySteppingSystem();
        final ServerLevel level = system.getLevel();

        final BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof final BeltBlockEntity belt))
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;

        final BlockState state = belt.getBlockState();
        final Direction facing = state.getValue(BeltBlock.HORIZONTAL_FACING);
        final BeltSlope slope = state.getValue(BeltBlock.SLOPE);
        if (slope == BeltSlope.SIDEWAYS)
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;

        final Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, facing.getAxis()).getNormal();
        float speed = belt.getBeltMovementSpeed() * 20.0f;

        if (facing.getAxis() == Direction.Axis.X) {
            speed *= -1.0f;
        }

        final Vector3d velocity = new Vector3d(normal.getX() * speed, normal.getY() * speed, normal.getZ() * speed);

        // TODO: do we need up/down force here? we probably should slant the collision boxes
//        if (velocity.lengthSquared() > 0.0) {
//            if (slope == BeltSlope.UPWARD) {
//                velocity.add(0.0, speed, 0.0).normalize(speed);
//            }
//            if (slope == BeltSlope.DOWNWARD) {
//                velocity.add(0.0,  -speed, 0.0).normalize(speed);
//            }
//        }

        if (slope == BeltSlope.HORIZONTAL && pos1.y - belt.getBlockPos().getY() < 0.5) {
            velocity.negate();
        }

        return new BlockSubLevelCollisionCallback.CollisionResult(velocity, false);
    }
}
