package dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle;

import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityRidingSubLevelVehicleHelper {

    /**
     * Returns the transformed, or "kicked" position of an entity riding a vehicle in a sub-level.
     * We transform the entity by its eye position instead of its normal position.
     */
    public static Vec3 kickRidingEntity(final Entity entity, final SubLevel subLevel) {
        if (EntitySubLevelUtil.shouldKick(entity)) {
            return kickRidingEntity(entity, entity.position(), subLevel);
        }

        return entity.position();
    }

    public static Vec3 kickRidingEntity(final Entity entity, final Vec3 position, final SubLevel subLevel) {
        final Vec3 eyePosition = entity.getEyePosition();
        final Vec3 feetPosition = entity.position();

        return subLevel.logicalPose().transformPosition(position.add(eyePosition.subtract(feetPosition))).add(feetPosition.subtract(eyePosition));
    }
}
