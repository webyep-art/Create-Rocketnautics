package dev.ryanhcode.sable.mixin.block_placement;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Disallows placing blocks on sub-levels inside of entities
 */
@Mixin(EntityGetter.class)
public interface EntityGetterMixin {

    @Shadow
    List<Entity> getEntities(@org.jetbrains.annotations.Nullable Entity pEntity, AABB pArea);

    @Shadow
    List<? extends Player> players();

    /**
     * @author RyanH
     * @reason Taking sub-levels into account
     */
    @Overwrite
    default boolean isUnobstructed(@Nullable final Entity pEntity, final VoxelShape voxelShape) {
        if (voxelShape.isEmpty()) {
            return true;
        } else {
            for (final Entity entity : this.getEntities(pEntity, voxelShape.bounds())) {
                final AABB entityBounds = entity.getBoundingBox();

                boolean fine = Shapes.joinIsNotEmpty(voxelShape, Shapes.create(entityBounds), BooleanOp.AND);

                final BoundingBox3d queryBounds = new BoundingBox3d(entityBounds);
                queryBounds.expand(1.5, queryBounds);
                final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(entity.level(), queryBounds);

                for (final SubLevel subLevel : intersecting) {
                    if (fine) continue;

                    final BoundingBox3d bb = new BoundingBox3d(entityBounds);
                    bb.transformInverse(subLevel.logicalPose(), bb);
                    bb.expand(-0.75 / 16.0, bb);
                    if (Shapes.joinIsNotEmpty(voxelShape, Shapes.create(bb.toMojang()), BooleanOp.AND))
                        fine = true;
                }

                if (!entity.isRemoved() && entity.blocksBuilding && (pEntity == null || !entity.isPassengerOfSameVehicle(pEntity)) && fine) {
                    return false;
                }
            }

            return true;
        }
    }

}
