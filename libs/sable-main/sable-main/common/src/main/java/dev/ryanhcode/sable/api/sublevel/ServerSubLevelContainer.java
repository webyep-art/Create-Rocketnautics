package dev.ryanhcode.sable.api.sublevel;


import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelOccupancySavedData;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Holds all sub-levels and plots in a {@link ServerLevel}
 */
public class ServerSubLevelContainer extends SubLevelContainer {

    /**
     * The physics system in this container
     */
    private @Nullable SubLevelPhysicsSystem physics;

    /**
     * The tracking system in this container
     */
    private @Nullable SubLevelTrackingSystem tracking;

    /**
     * The holding chunk map for this sub-level container.
     */
    private SubLevelHoldingChunkMap holdingChunkMap;

    /**
     * Creates a new sub-level container with the given side length and plot size.
     *
     * @param level         the level of the plotgrid
     * @param logSideLength the log_2 of the amount of chunks in the side of the plotgrid
     * @param logPlotSize   the log_2 of the amount of chunks in the side of a plot
     * @param originX       the X coordinate in plots of the origin of the plotgrid
     * @param originZ       the Z coordinate in plots of the origin of the plotgrid
     */
    public ServerSubLevelContainer(final Level level, final int logSideLength, final int logPlotSize, final int originX, final int originZ) {
        super(level, logSideLength, logPlotSize, originX, originZ);
    }

    /**
     * Initialize after method construction is done
     */
    public void initialize() {
        this.holdingChunkMap = new SubLevelHoldingChunkMap(this.getLevel(), this);
    }

    /**
     * Called every tick for the plotgrid.
     */
    @Override
    public void tick() {
        super.tick();
        this.holdingChunkMap.processChanges();
    }

    /**
     * Sets the internal physics system.
     */
    @ApiStatus.Internal
    public void takePhysicsSystem(final SubLevelPhysicsSystem physics) {
        this.physics = physics;
    }

    /**
     * Sets the internal tracking system.
     */
    @ApiStatus.Internal
    public void takeTrackingSystem(final SubLevelTrackingSystem tracking) {
        this.tracking = tracking;
    }

    /**
     * @return the physics pipeline in this container
     */
    public @NotNull SubLevelPhysicsSystem physicsSystem() {
        assert this.physics != null;
        return this.physics;
    }

    /**
     * @return the physics pipeline in this container
     */
    public @NotNull SubLevelTrackingSystem trackingSystem() {
        assert this.tracking != null;
        return this.tracking;
    }

    /**
     * Removes a sub-level with a local plot coordinate
     */
    @Override
    public void removeSubLevel(final int x, final int z, final SubLevelRemovalReason reason) {
        final ServerSubLevel subLevel = (ServerSubLevel) this.getSubLevel(x, z);
        if (subLevel == null) {
            throw new IllegalStateException("No sub-level at " + x + ", " + z);
        }

        if (reason == SubLevelRemovalReason.REMOVED) {
            subLevel.deleteAllEntities();
        }

        super.removeSubLevel(x, z, reason);

        if (reason == SubLevelRemovalReason.REMOVED) {
            final ServerLevel level = this.getLevel();
            SubLevelOccupancySavedData.getOrLoad(level).setDirty();
            this.holdingChunkMap.queueDeletion(subLevel);
        }
    }

    @Override
    protected SubLevel createSubLevel(final int globalPlotX, final int globalPlotZ, final Pose3d pose, final UUID uuid) {
        final ServerLevel level = this.getLevel();
        final ServerSubLevel subLevel = new ServerSubLevel(level, globalPlotX, globalPlotZ, pose);
        subLevel.setUniqueId(uuid);

        final Vector3d position = pose.position();
        final BlockPos blockPos = BlockPos.containing(position.x, position.y, position.z);

        if (level.isLoaded(blockPos)) {
            final Holder<Biome> holder = level.getBiome(blockPos);
            final Optional<ResourceKey<Biome>> key = holder.unwrapKey();

            //noinspection OptionalIsPresent
            if (key.isPresent()) {
                subLevel.getPlot().setBiome(key.get());
            }
        }

        return subLevel;
    }

    public SubLevelHoldingChunkMap getHoldingChunkMap() {
        return this.holdingChunkMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ServerSubLevel> getAllSubLevels() {
        return (List<ServerSubLevel>) super.getAllSubLevels();
    }

    /**
     * @return the level of the plotgrid.
     */
    @Override
    public ServerLevel getLevel() {
        return (ServerLevel) super.getLevel();
    }

    /**
     * Frees all native resources
     */
    public void close() {
        try {
            this.holdingChunkMap.close();
        } catch (final Exception e) {
            Sable.LOGGER.error("Failed closing sub-level holding chunk map", e);
        }
    }
}
