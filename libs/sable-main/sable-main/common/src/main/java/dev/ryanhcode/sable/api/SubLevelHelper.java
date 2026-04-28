package dev.ryanhcode.sable.api;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinterface.EntityExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;
import java.util.function.BiFunction;

/**
 * A helper class for handling interactions between sub-levels<->sub-levels and sub-levels<->levels
 */
@ApiStatus.Internal
public final class SubLevelHelper {

    private static final ThreadLocal<EntityRot> oldRot = ThreadLocal.withInitial(EntityRot::new);
    private static final ObjectList<BiFunction<Vector3dc, Level, Vector3dc>> windProviders = new ObjectArrayList<>();

    /**
     * Projects a full entity into a subLevel, rotation and all.
     * Pushing an entity into local space of a sub-level caches old values for its old rotations.
     * As such, it is important to call {@link SubLevelHelper#popEntityLocal} after calling {@link SubLevelHelper#pushEntityLocal} before pushing another entity.
     * Projects the entity position, not the eye position.
     *
     * @param subLevel The subLevel to project into
     * @param entity   The entity to project
     */
    public static void pushEntityLocal(final SubLevel subLevel, final Entity entity) {
        SubLevelHelper.pushEntityLocal(subLevel, entity, EntityAnchorArgument.Anchor.FEET);
    }

    /**
     * Projects a full entity out of a subLevel, rotation and all.
     * Uses the cached values from {@link SubLevelHelper#pushEntityLocal}.
     * Projects the entity position, not the eye position.
     *
     * @param subLevel The subLevel to project out of
     * @param player   The entity to project
     */
    public static void popEntityLocal(final SubLevel subLevel, final Entity player) {
        SubLevelHelper.popEntityLocal(subLevel, player, EntityAnchorArgument.Anchor.FEET);
    }

    /**
     * Projects a full entity into a subLevel, rotation and all.
     * Pushing an entity into local space of a sub-level caches old values for its old rotations.
     * As such, it is important to call {@link SubLevelHelper#popEntityLocal} after calling {@link SubLevelHelper#pushEntityLocal} before pushing another entity.
     *
     * @param subLevel The subLevel to project into
     * @param entity   The entity to project
     * @param anchor   The anchor that should be projected
     */
    public static void pushEntityLocal(final SubLevel subLevel, final Entity entity, final EntityAnchorArgument.Anchor anchor) {
        if (anchor == EntityAnchorArgument.Anchor.FEET) {
            ((EntityExtension) entity).sable$setPosSuperRaw(subLevel.logicalPose().transformPositionInverse(entity.position()));
        } else {
            ((EntityExtension) entity).sable$setPosSuperRaw(subLevel.logicalPose().transformPositionInverse(entity.getEyePosition()).add(0.0, -entity.getEyeHeight(), 0.0));
        }

        Vec3 playerLookAngle = entity.getLookAngle();
        playerLookAngle = subLevel.logicalPose().transformNormalInverse(playerLookAngle);
        oldRot.get().copy(entity);

        final Vec3 pTarget = entity.getEyePosition().add(playerLookAngle);
        final Vec3 vec3 = entity.getEyePosition();
        final double d0 = pTarget.x - vec3.x;
        final double d1 = pTarget.y - vec3.y;
        final double d2 = pTarget.z - vec3.z;
        final double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        entity.setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * (double) (180F / (float) Math.PI)))));
        entity.setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F));
        entity.setYHeadRot(entity.getYRot());

        entity.setDeltaMovement(subLevel.logicalPose().transformNormalInverse(entity.getDeltaMovement()));
    }

    /**
     * Projects a full entity out of a subLevel, rotation and all.
     * Uses the cached values from {@link SubLevelHelper#pushEntityLocal}.
     *
     * @param subLevel The subLevel to project out of
     * @param entity   The entity to project
     * @param anchor   The anchor that should be projected
     */
    public static void popEntityLocal(final SubLevel subLevel, final Entity entity, final EntityAnchorArgument.Anchor anchor) {
        if (anchor == EntityAnchorArgument.Anchor.FEET) {
            ((EntityExtension) entity).sable$setPosSuperRaw(subLevel.logicalPose().transformPosition(entity.position()));
        } else {
            ((EntityExtension) entity).sable$setPosSuperRaw(subLevel.logicalPose().transformPosition(entity.getEyePosition()).add(0.0, -entity.getEyeHeight(), 0.0));
        }

        oldRot.get().apply(entity);
        entity.setDeltaMovement(subLevel.logicalPose().transformNormal(entity.getDeltaMovement()));
    }

    /**
     * Gets the global velocity of a point in a level relative to the air, taking into account sublevels and their plots/poses
     *
     * @param level the level to check
     * @param pos   the position of the point
     * @param dest  the vector to hold the result
     * @return the global velocity of the point stored in dest [m/s]
     */
    public static Vector3d getVelocityRelativeToAir(final Level level, final Vector3dc pos, final Vector3d dest) {
        final Vector3d probePos = new Vector3d(pos);
        final Vector3d velocity = Sable.HELPER.getVelocity(level, pos, dest);

        for (final BiFunction<Vector3dc, Level, Vector3dc> windProvider : windProviders) {
            final Vector3dc airVelocity = windProvider.apply(probePos, level);

            if (airVelocity != null) {
                velocity.sub(airVelocity);
            }
        }

        return velocity;
    }

    /**
     * Registers a function to get the air velocity of a point in a level
     *
     * @param function the function to register
     */
    public static void registerWindProvider(final BiFunction<Vector3dc, Level, Vector3dc> function) {
        windProviders.add(function);
    }

    /**
     * @return the chain of sub-levels that should load / unload with the given one
     */
    public static Collection<ServerSubLevel> getLoadingDependencyChain(final ServerSubLevel subLevel) {
        final ObjectOpenHashSet<ServerSubLevel> visited = new ObjectOpenHashSet<>();
        final ObjectOpenHashSet<ServerSubLevel> frontier = new ObjectOpenHashSet<>();

        frontier.add(subLevel);

        while (!frontier.isEmpty()) {
            final ServerSubLevel current = frontier.iterator().next();

            frontier.remove(current);
            visited.add(current);

            final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(current.getLevel(), new BoundingBox3d(current.boundingBox()));

            // Intersecting dependencies
            for (final SubLevel neighbor : intersecting) {
                final ServerSubLevel serverNeighbor = (ServerSubLevel) neighbor;

                if (!visited.contains(serverNeighbor)) {
                    frontier.add(serverNeighbor);
                }
            }

            // Actor dependencies
            for (final BlockEntitySubLevelActor actor : current.getPlot().getBlockEntityActors()) {
                final Iterable<SubLevel> loadingDependencies = actor.sable$getLoadingDependencies();

                if (loadingDependencies == null) continue;

                for (final SubLevel dependency : loadingDependencies) {
                    final ServerSubLevel serverDependency = (ServerSubLevel) dependency;

                    if (!visited.contains(serverDependency)) {
                        frontier.add(serverDependency);
                    }
                }
            }

        }

        return visited;
    }

    /**
     * @return the chain of sub-levels considered connected
     */
    public static Collection<SubLevel> getConnectedChain(final SubLevel subLevel) {
        final ObjectOpenHashSet<SubLevel> visited = new ObjectOpenHashSet<>();
        final ObjectOpenHashSet<SubLevel> frontier = new ObjectOpenHashSet<>();

        frontier.add(subLevel);

        while (!frontier.isEmpty()) {
            final SubLevel current = frontier.iterator().next();

            frontier.remove(current);
            visited.add(current);

            // Actor dependencies
            for (final BlockEntitySubLevelActor actor : current.getPlot().getBlockEntityActors()) {
                final Iterable<SubLevel> dependencies = actor.sable$getConnectionDependencies();

                if (dependencies == null) continue;

                for (final SubLevel dependency : dependencies) {
                    final SubLevel serverDependency = dependency;

                    if (!visited.contains(serverDependency)) {
                        frontier.add(serverDependency);
                    }
                }
            }
        }

        return visited;
    }

    private static class EntityRot {

        private float xRot;
        private float yRot;
        private float yHeadRot;

        public void apply(final Entity entity) {
            entity.setXRot(this.xRot);
            entity.setYRot(this.yRot);
            entity.setYHeadRot(this.yHeadRot);
        }

        public void copy(final Entity entity) {
            this.xRot = entity.getXRot();
            this.yRot = entity.getYRot();
            this.yHeadRot = entity.getYHeadRot();
        }
    }
}
