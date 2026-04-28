package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportVisual;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.frogports.FrogportMixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes frogports face & extend the proper amount when picking up and depositing cross-sub-level
 */
@Mixin(FrogportVisual.class)
public class FrogportVisualMixin {

    @WrapOperation(method = "animate", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packagePort/PackagePortTarget;getExactTargetLocation(Lcom/simibubi/create/content/logistics/packagePort/PackagePortBlockEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 sable$getExactTargetLocation(final PackagePortTarget instance,
                                             final PackagePortBlockEntity packagePortBlockEntity,
                                             final LevelAccessor levelAccessor,
                                             final BlockPos blockPos,
                                             final Operation<Vec3> original) {
        return FrogportMixinHelper.getExactTargetLocation(instance, packagePortBlockEntity, levelAccessor, blockPos, original);
    }

}
