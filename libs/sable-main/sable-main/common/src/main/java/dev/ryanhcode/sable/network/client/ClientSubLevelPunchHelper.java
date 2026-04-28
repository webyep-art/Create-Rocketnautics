package dev.ryanhcode.sable.network.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.index.SableAttributes;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundPunchSubLevelPacket;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3d;

public class ClientSubLevelPunchHelper {
    /**
     * @param testCreativeBreaking whether to prevent pushing if a creative player will destroy the block
     */
    public static void clientTryPunch(final BlockHitResult hitResult, final Level level, final boolean testCreativeBreaking) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player.blockActionRestricted(level, hitResult.getBlockPos(), minecraft.gameMode.getPlayerMode()) ||
                player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
            return;
        }

        if (player.isCreative() && testCreativeBreaking) {
            final BlockState blockState = minecraft.level.getBlockState(hitResult.getBlockPos());

            if (player.getMainHandItem().getItem().canAttackBlock(blockState, minecraft.level, hitResult.getBlockPos(), player)) {
                return;
            }
        }

        final Vector3d hitPosition = JOMLConversion.toJOML(hitResult.getLocation());
        final Vector3d hitDirection = JOMLConversion.toJOML(player.getLookAngle());

        final SubLevel targetSubLevel = Sable.HELPER.getContaining(level, hitPosition);
        final SubLevel trackingSubLevel = ((EntityMovementExtension)player).sable$getTrackingSubLevel();

        // do not try to punch the sublevel you are standing on. do not pass go. do not collect $200
        if (targetSubLevel == trackingSubLevel) {
            return;
        }

        // if we're punching a sub-level, store the hit position relative to its clientside COM
        if (targetSubLevel != null) {
            targetSubLevel.lastPose().transformPosition(hitPosition);
            hitPosition.sub(targetSubLevel.lastPose().position());
        }

        // if we're standing on a sub-level, store the hit direction relative to its clientside orientation
        if (trackingSubLevel != null) {
            trackingSubLevel.lastPose().transformNormalInverse(hitDirection);
        }

        final int customCooldown = SableAttributes.getPushCooldownTicks(player);
        if (customCooldown > 0) {
            player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), customCooldown);
        }

        minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new ServerboundPunchSubLevelPacket(
                hitResult.getBlockPos(), hitPosition, hitDirection
        )));
    }
}
