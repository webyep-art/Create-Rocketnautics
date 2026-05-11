package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract double getY();

    @ModifyReturnValue(method = "getGravity", at = @At("RETURN"))
    private double rocketnautics$applyLowGravity(double original) {
        return original * (1 - GlobalSpacePhysicsHandler.calculateGravityFactor(level(), getY()));
    }
}
