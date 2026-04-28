package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.elevator_controls;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes the range check on Elevator Controls in Create to take into account sub-levels
 */
@Mixin(ElevatorControlsHandler.class)
public class ElevatorControlsHandlerMixin {

    @Redirect(method = "onScroll",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB sable$projectAABB(final AbstractContraptionEntity instance) {
        final SubLevel subLevel = Sable.HELPER.getContaining(instance.level(), instance.getBoundingBox().getCenter());
        final AABB projectedBB = instance.getBoundingBox();

        if (subLevel != null) {
            final BoundingBox3d bb = new BoundingBox3d(projectedBB);
            return bb.transform(subLevel.logicalPose(), bb).toMojang();
        }

        return projectedBB;
    }
}
