package dev.ryanhcode.sable.mixin.entity.entity_unloading;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Fix seats unloading on sub-levels
 */
@Mixin(PersistentEntitySectionManager.class)
public class PersistentEntitySectionManagerMixin {

    @Shadow
    @Final
    public EntitySectionStorage<EntityAccess> sectionStorage;

    @Inject(method = "processChunkUnload", at = @At("HEAD"))
    private void processChunkUnload(final long l, final CallbackInfoReturnable<Boolean> cir) {
        final List<EntitySection<EntityAccess>> sections = this.sectionStorage
                .getExistingSectionsInChunk(l)
                .toList();

        for (final EntitySection<EntityAccess> section : sections) {
            final List<EntityAccess> entities = section.getEntities().toList();

            for (final EntityAccess entityAccess : entities) {
                final Entity entity = ((Entity) entityAccess);
                final boolean inPlot = SubLevelContainer.getContainer(entity.level()).inBounds(entity.chunkPosition());

                if (inPlot && (entity.getRemovalReason() == null || entity.getRemovalReason().shouldSave())) {
                    if (entity.isVehicle() && entity.hasExactlyOnePlayerPassenger()) {
                        entity.getPassengers().getFirst().removeVehicle();
                    }
                }
            }
        }
    }
}
