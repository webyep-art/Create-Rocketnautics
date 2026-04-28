package dev.ryanhcode.sable.physics.callback;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class BeehiveBlockCallback extends FragileBlockCallback {
    public static final BeehiveBlockCallback INSTANCE = new BeehiveBlockCallback();

    public BeehiveBlockCallback() {}

    @Override
    public boolean shouldTriggerFor(final BlockState state) {
        return state.getBlock() instanceof BeehiveBlock;
    }

    @Override
    public double getTriggerVelocity() {
        return 9.0;
    }

    @Override
    public CollisionResult onHit(final ServerLevel level, final BlockPos pos, final BlockState state, final Vector3d hitPos) {
        final BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof final BeehiveBlockEntity beehiveBlockEntity) {
            final Vec3 center = pos.getCenter();

            final Player nearbyPlayer = level.getNearestPlayer(center.x, center.y, center.z, 4, true);
            beehiveBlockEntity.emptyAllLivingFromHive(nearbyPlayer, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        }

        return new CollisionResult(JOMLConversion.ZERO, false);
    }
}
