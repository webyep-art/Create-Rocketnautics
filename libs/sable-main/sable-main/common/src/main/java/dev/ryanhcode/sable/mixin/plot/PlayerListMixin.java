package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Forces distance-based packet broadcasting to take into account sublevels
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Shadow
    @Final
    private List<ServerPlayer> players;

    /**
     * @author RyanH
     * @reason Overwrite to make distance checks take into account sublevels
     */
    @Overwrite
    public void broadcast(@Nullable final Player player, final double x, final double y, final double z, final double maxDistance, final ResourceKey<Level> resourceKey, final Packet<?> packet) {
        final ActiveSableCompanion helper = Sable.HELPER;
        for (final ServerPlayer value : this.players) {
            final Level level = value.level();
            if (value != player && level.dimension() == resourceKey) {
                final double dist = helper.distanceSquaredWithSubLevels(level, value.position(), x, y, z);

                if (dist < maxDistance * maxDistance) {
                    value.connection.send(packet);
                }
            }
        }
    }
}


