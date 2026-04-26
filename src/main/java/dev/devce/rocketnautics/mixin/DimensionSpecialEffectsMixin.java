package dev.devce.rocketnautics.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionSpecialEffects.class)
public class DimensionSpecialEffectsMixin {

    @Inject(method = "getBrightnessDependentFogColor", at = @At("RETURN"), cancellable = true)
    private void rocketnautics$forceSpaceFogColor(Vec3 fogColor, float brightness, CallbackInfoReturnable<Vec3> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double y = mc.player.getY();
        if (y > 300) {
            float factor = (float) Mth.clamp((y - 300.0) / 700.0, 0.0, 1.0);
            Vec3 color = cir.getReturnValue();
            
            // For testing: interpolating to RED instead of black to see if it works
            double r = Mth.lerp(factor, color.x, 1.0); 
            double g = Mth.lerp(factor, color.y, 0.0);
            double b = Mth.lerp(factor, color.z, 0.0);
            
            cir.setReturnValue(new Vec3(r, g, b));
        }
    }
}
