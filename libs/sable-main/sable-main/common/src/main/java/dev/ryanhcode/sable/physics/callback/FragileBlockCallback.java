package dev.ryanhcode.sable.physics.callback;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public class FragileBlockCallback implements BlockSubLevelCollisionCallback {

    public static final FragileBlockCallback INSTANCE = new FragileBlockCallback();

    protected FragileBlockCallback() {}

    public double getTriggerVelocity() {
        return 4.0;
    }

    @Override
    public BlockSubLevelCollisionCallback.CollisionResult sable$onCollision(final BlockPos pos, final Vector3d pos1, final double impactVelocity) {
        final double triggerVelocity = this.getTriggerVelocity();

        if (impactVelocity * impactVelocity < triggerVelocity * triggerVelocity) {
            return CollisionResult.NONE;
        }

        final SubLevelPhysicsSystem system = SubLevelPhysicsSystem.getCurrentlySteppingSystem();
        final ServerLevel level = system.getLevel();

        // Double check that we're actually fragile before breaking (in-case pipeline gave us a slightly off collision position)
        final BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof LeavesBlock && state.getValue(LeavesBlock.PERSISTENT))
            return CollisionResult.NONE;

        if (this.shouldTriggerFor(state)) {
            return this.onHit(level, pos, state, pos1);
        }

        return new CollisionResult(JOMLConversion.ZERO, true);
    }

    public boolean shouldTriggerFor(final BlockState state) {
        return ((BlockStateExtension) state).sable$getProperty(PhysicsBlockPropertyTypes.FRAGILE.get());
    }

    public CollisionResult onHit(final ServerLevel level, final BlockPos pos, final BlockState state, final Vector3d hitPos) {
        level.destroyBlock(pos, true);

        // Melt ice on destruction
        if (state.getBlock() instanceof IceBlock) {
            final BlockState belowState = level.getBlockState(pos.below());

            if (belowState.blocksMotion() || belowState.liquid()) {
                level.setBlockAndUpdate(pos, IceBlock.meltsInto());
            }
        }

        return new CollisionResult(JOMLConversion.ZERO, true);
    }
}
