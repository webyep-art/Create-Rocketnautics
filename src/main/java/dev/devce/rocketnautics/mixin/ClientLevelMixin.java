package dev.devce.rocketnautics.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void rocketnautics$darkenSkyAtAltitude(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        // Use the actual position passed to the method
        double y = pos.y; 
        
        // Safety buffer: strictly vanilla below 800
        if (y > 800) {
            float factor = (float) Mth.clamp((y - 1000.0) / 2000.0, 0.0, 1.0);
            if (factor <= 0.0f) return;

            Vec3 originalColor = cir.getReturnValue();
            double r = Mth.lerp(factor, originalColor.x, 0.0);
            double g = Mth.lerp(factor, originalColor.y, 0.0);
            double b = Mth.lerp(factor, originalColor.z, 0.0);
            
            cir.setReturnValue(new Vec3(r, g, b));
        }
    }
}
