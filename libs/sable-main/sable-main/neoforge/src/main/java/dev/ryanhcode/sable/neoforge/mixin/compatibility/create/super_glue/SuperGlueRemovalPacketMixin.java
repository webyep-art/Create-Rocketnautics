package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.super_glue;

import com.simibubi.create.content.contraptions.glue.SuperGlueRemovalPacket;
import dev.ryanhcode.sable.Sable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes the range check in Create's {@link SuperGlueRemovalPacket} handling to account for sub-levels.
 */
@Mixin(SuperGlueRemovalPacket.class)
public class SuperGlueRemovalPacketMixin {

    @Redirect(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private double sable$distanceSquared(final ServerPlayer instance, final Vec3 vec3) {
        return Sable.HELPER.distanceSquaredWithSubLevels(instance.level(), instance.position(), vec3);
    }
}
