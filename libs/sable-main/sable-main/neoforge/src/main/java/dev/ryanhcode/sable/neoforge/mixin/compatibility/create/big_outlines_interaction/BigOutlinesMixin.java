package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.big_outlines_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.block.BigOutlines;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.raycasts.SableRaycastHelper;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/**
 * Makes it so big outline tracing (which affects mc.hitResult) is able to target sublevels
 */
@Mixin(BigOutlines.class)
public class BigOutlinesMixin {

    /**
     * Needed to pass the sublevel from the function to its lambda,
     * This code is client side only so this should be thread safe
     *
     */
    @Unique
    private static ClientSubLevel sable$predicateSubLevel = null;

    @WrapOperation(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double sable$modifyMaxRange(Vec3 worldHitPos,
                                               final Vec3 origin,
                                               final Operation<Double> original,
                                               @Local final Minecraft minecraft) {
        final ClientSubLevel containing = Sable.HELPER.getContainingClient(worldHitPos);
        final float pt = AnimationTickHolder.getPartialTicks(minecraft.level);

        if (containing != null) {
            worldHitPos = containing.renderPose(pt).transformPosition(worldHitPos);
        }

        return original.call(worldHitPos, origin);
    }

    @Redirect(method = "pick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/RaycastHelper;rayTraceUntil(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;)Lcom/simibubi/create/foundation/utility/RaycastHelper$PredicateTraceResult;"))
    private static RaycastHelper.PredicateTraceResult sable$useSubLevelInclusiveCast(final Vec3 worldOrigin,
                                                                                     final Vec3 worldTarget,
                                                                                     final Predicate<BlockPos> predicate,
                                                                                     @Local final Minecraft minecraft) {
        return SableRaycastHelper.rayCastUntilWithSublevels(minecraft.level, worldOrigin, worldTarget, (subLevel, pos) -> {
            sable$predicateSubLevel = (ClientSubLevel) subLevel;
            return predicate.test(pos);
        });
    }

    @WrapOperation(
            method = "lambda$pick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/shapes/VoxelShape;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;"
            )
    )
    private static BlockHitResult sable$clipUsingLocalSubLevel(final VoxelShape instance,
                                                               final Vec3 origin,
                                                               final Vec3 target,
                                                               final BlockPos blockPos,
                                                               final Operation<BlockHitResult> original) {
        final float pt = AnimationTickHolder.getPartialTicks(Minecraft.getInstance().level);

        if (sable$predicateSubLevel == null) {
            return original.call(instance, origin, target, blockPos);
        } else {
            final Vec3 localOrigin = sable$predicateSubLevel.renderPose(pt).transformPositionInverse(origin);
            final Vec3 localTarget = sable$predicateSubLevel.renderPose(pt).transformPositionInverse(target);
            return original.call(instance, localOrigin, localTarget, blockPos);
        }
    }

    @WrapOperation(
            method = "lambda$pick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
            )
    )
    private static double sable$distanceToWithSubLevel(final Vec3 instance,
                                                       final Vec3 origin,
                                                       final Operation<Double> original) {
        final float pt = AnimationTickHolder.getPartialTicks(Minecraft.getInstance().level);

        if (sable$predicateSubLevel == null) {
            return original.call(instance, origin);
        } else {
            final Vec3 localOrigin = sable$predicateSubLevel.renderPose(pt).transformPositionInverse(origin);
            return original.call(instance, localOrigin);
        }
    }

}
