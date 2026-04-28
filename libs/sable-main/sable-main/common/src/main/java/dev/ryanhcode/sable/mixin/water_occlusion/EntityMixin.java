package dev.ryanhcode.sable.mixin.water_occlusion;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes entities not think they're in water when in occluded regions
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private Level level;

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow public abstract double getX();

    @Shadow public abstract double getEyeY();

    @Shadow public abstract double getZ();

    @Inject(method = "updateFluidHeightAndDoFluidPushing", at = @At("HEAD"), cancellable = true)
    public void sable$updateFluidHeightAndDoFluidPushing(final TagKey<Fluid> tagKey, final double d, final CallbackInfoReturnable<Boolean> cir) {
        final boolean occluded = this.sable$isOccluded();

        if (occluded) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean sable$isOccluded() {
        final WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(this.level);

        if (container == null)
            return false;

        return container.isOccluded(this.getBoundingBox().getCenter()) ||
                container.isOccluded(this.position());
    }

    @WrapOperation(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSwimming(Z)V"))
    public void sable$inWaterCheck(final Entity instance, final boolean swimming, final Operation<Void> original) {
        if (swimming && this.sable$isOccluded()) {
            original.call(instance, false);
            return;
        }

        original.call(instance, swimming);
    }

    @WrapOperation(method = "updateFluidOnEyes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    public FluidState sable$occludeFluidOnEyes(final Level instance, final BlockPos pos, final Operation<FluidState> original) {
        final FluidState originalState = original.call(instance, pos);

        final WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(this.level);

        if (!originalState.isEmpty() && container != null && container.isOccluded(new Vec3(this.getX(), this.getEyeY(), this.getZ()))) {
            // If we're occluded, we don't want to return a fluid state
            return Fluids.EMPTY.defaultFluidState();
        }

        return originalState;
    }
}
