package dev.ryanhcode.sable.mixin.block_placement;


import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes the rotation of block placement to take into account orientation
 */
@Mixin(UseOnContext.class)
public abstract class UseOnContextMixin {

    @Shadow
    @Final
    private Level level;
    @Shadow
    @Final
    @Nullable
    private Player player;

    @Shadow
    public abstract BlockPos getClickedPos();

    @Inject(method = "getHorizontalDirection", at = @At("HEAD"), cancellable = true)
    private void sable$getHorizontalDirection(final CallbackInfoReturnable<Direction> cir) {
        if (this.player == null) return;

        final SubLevel subLevel = Sable.HELPER.getContaining(this.level, this.getClickedPos());

        if (subLevel != null) {
            SubLevelHelper.pushEntityLocal(subLevel, this.player);
            final Direction dir = this.player.getDirection();
            SubLevelHelper.popEntityLocal(subLevel, this.player);
            cir.setReturnValue(dir);
        }
    }

    @Inject(method = "getRotation", at = @At("HEAD"), cancellable = true)
    private void sable$getRotation(final CallbackInfoReturnable<Float> cir) {
        if (this.player == null) return;

        final SubLevel subLevel = Sable.HELPER.getContaining(this.level, this.getClickedPos());

        if (subLevel != null) {
            SubLevelHelper.pushEntityLocal(subLevel, this.player);
            final float yRot = this.player.getYRot();
            SubLevelHelper.popEntityLocal(subLevel, this.player);
            cir.setReturnValue(yRot);
        }
    }

}
