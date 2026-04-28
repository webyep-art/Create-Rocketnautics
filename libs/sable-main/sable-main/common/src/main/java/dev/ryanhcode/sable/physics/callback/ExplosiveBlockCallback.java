package dev.ryanhcode.sable.physics.callback;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public class ExplosiveBlockCallback extends FragileBlockCallback {
    public static final ExplosiveBlockCallback INSTANCE = new ExplosiveBlockCallback();

    public ExplosiveBlockCallback() {}

    @Override
    public boolean shouldTriggerFor(final BlockState state) {
        return state.getBlock() instanceof TntBlock;
    }

    @Override
    public double getTriggerVelocity() {
        return 5.0;
    }

    @Override
    public CollisionResult onHit(final ServerLevel level, final BlockPos pos, final BlockState state, final Vector3d hitPos) {
        final PrimedTnt primedTnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, null);
        primedTnt.setFuse(4);
        level.addFreshEntity(primedTnt);

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        return new CollisionResult(JOMLConversion.ZERO, true);
    }
}
