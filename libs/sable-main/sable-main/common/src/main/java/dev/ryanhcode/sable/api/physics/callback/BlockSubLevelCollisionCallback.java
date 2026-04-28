package dev.ryanhcode.sable.api.physics.callback;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface BlockSubLevelCollisionCallback {

    /**
     * Called when a collision occurs between two blocks, from JNI / pipeline implementations
     *
     * @return tangent motion
     */
    @ApiStatus.Internal
    @SuppressWarnings("unused")
    default double[] onCollision(final int x,
                                 final int y,
                                 final int z,
                                 final double x1,
                                 final double y1,
                                 final double z1,
                                 final double impactVelocity) {
        final CollisionResult result = this.sable$onCollision(new BlockPos(x, y, z), new Vector3d(x1, y1, z1), impactVelocity);
        final Vector3dc motion = result.tangentMotion;

        // TODO: this is stupid and moronic to pass through the removal as a double lmao, let's not do that in the future
        return new double[]{motion.x(), motion.y(), motion.z(), result.removeCollision ? 1.0 : 0.0};
    }

    CollisionResult sable$onCollision(BlockPos blockPos, Vector3d pos, double impactVelocity);

    record CollisionResult(Vector3dc tangentMotion, boolean removeCollision) {
        public static final CollisionResult NONE = new CollisionResult(JOMLConversion.ZERO, false);
    }

}
