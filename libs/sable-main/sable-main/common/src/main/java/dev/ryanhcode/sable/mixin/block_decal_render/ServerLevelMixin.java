package dev.ryanhcode.sable.mixin.block_decal_render;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Fixes {@link net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket ClientboundBlockDestructionPackets} not being sent to players outside of a hardcoded range
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @ModifyConstant(method = "destroyBlockProgress", constant = @Constant(doubleValue = 1024.0, ordinal = 0))
    private double sable$blockDamageDistance(final double originalBlockDamageDistanceConstant) {
        return Double.MAX_VALUE;
    }

}
