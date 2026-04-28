package dev.ryanhcode.sable.mixin.world_border;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.world_border.WorldBorderExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.class)
public class WorldBorderMixin implements WorldBorderExtension {

    @Unique
    private Level sable$level;

    @Inject(method = "isWithinBounds(DDD)Z", at = @At("HEAD"), cancellable = true)
    public void sable$isWithinBounds(final double x, final double z, final double offset, final CallbackInfoReturnable<Boolean> cir) {
        if (this.sable$level == null) {
            return;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(this.sable$level);

        if (container != null && container.inBounds(Mth.floor(x) >> 4, Mth.floor(z) >> 4)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "clampToBounds(DDD)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void sable$clampToBounds(final double x, final double y, final double z, final CallbackInfoReturnable<BlockPos> cir) {
        if (this.sable$level == null) {
            return;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(this.sable$level);

        if (container != null && container.inBounds(Mth.floor(x) >> 4, Mth.floor(z) >> 4)) {
            cir.setReturnValue(BlockPos.containing(x, y, z));
        }
    }

    @Inject(method = "isInsideCloseToBorder", at = @At("HEAD"), cancellable = true)
    public void sable$isInsideCloseToBorder(final Entity entity, final AABB aABB, final CallbackInfoReturnable<Boolean> cir) {
        if (this.sable$level == null) {
            return;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(this.sable$level);

        if (container != null && Sable.HELPER.getContaining(entity) != null) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void sable$setLevel(final Level level) {
        this.sable$level = level;
    }
}
