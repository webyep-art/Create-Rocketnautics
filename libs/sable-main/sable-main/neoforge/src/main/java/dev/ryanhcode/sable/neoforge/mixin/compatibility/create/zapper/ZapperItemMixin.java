package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.zapper;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.equipment.zapper.ZapperItem;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the beam dispensed from Create's {@link ZapperItem} appear in the correct location
 */
@Mixin(ZapperItem.class)
public class ZapperItemMixin {

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/equipment/zapper/ShootableGadgetItemMethods;applyCooldown(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Ljava/util/function/Predicate;I)V"))
    private void sable$projectTargetPos(final Level world, final Player player, final InteractionHand hand, final CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, @Local final LocalRef<BlockHitResult> raytrace) {
        final BlockHitResult blockHitResult = raytrace.get();

        raytrace.set(new BlockHitResult(
                Sable.HELPER.projectOutOfSubLevel(world, blockHitResult.getLocation()),
                blockHitResult.getDirection(),
                blockHitResult.getBlockPos(),
                blockHitResult.isInside()
        ));
    }

}
