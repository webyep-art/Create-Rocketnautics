package dev.ryanhcode.sable.mixin.entity.entity_kicking;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Supplier;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Shadow
    private static void popResource(final Level arg, final Supplier<ItemEntity> supplier, final ItemStack arg2) {
    }

    @Inject(method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private static void sable$popResourceFromFace(final Level level, final BlockPos blockPos, final ItemStack itemStack, final CallbackInfo ci, final double yOffset, final double x, final double y, final double z) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, blockPos);

        if (subLevel != null) {
            popResource(level, () -> {
                final ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);

                Vec3 deltaMovement = itemEntity.getDeltaMovement();

                deltaMovement = subLevel.logicalPose().transformNormalInverse(deltaMovement);

                itemEntity.setDeltaMovement(deltaMovement);

                return itemEntity;
            }, itemStack);

            ci.cancel();
        }
    }

}
