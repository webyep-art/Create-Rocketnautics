package dev.ryanhcode.sable.mixin.sublevel_sounds;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes sound distance checks take into account sublevels
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Redirect(method = "playSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(DDD)D"))
    private double sable$playSound(final Vec3 instance, final double x, final double y, final double z) {
        return Sable.HELPER.distanceSquaredWithSubLevels(Minecraft.getInstance().level, instance, x, y, z);
    }

}
