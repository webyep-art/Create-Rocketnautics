package dev.ryanhcode.sable.sublevel.storage.holding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.toast.SableToastableServer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SubLevelHoldingChunkMap implements AutoCloseable {
    public static boolean VERBOSE = false;
    private final ServerLevel level;
    private final ServerSubLevelContainer container;

    private final SubLevelStorage storage;

    /**
     * All sub-levels currently in holding, queryable by UUID so that systems such as tracking points can locate sub-levels for poses
     */
    private final Object2ObjectMap<UUID, HoldingSubLevel> allHoldingSubLevels = new Object2ObjectOpenHashMap<>();

    /**
     * All currently loaded sub-level holding chunks
     */
    private final Long2ObjectMap<SubLevelHoldingChunk> loadedHoldingChunks = new Long2ObjectOpenHashMap<>();

    /**
     * The set of all dirty sub-level holding chunks, that need to be saved during the next autosave
     */
    private final LongSet dirtyHoldingChunks = new LongOpenHashSet();

    /**
     * The set of all holding chunks that can be gotten rid of after the next auto-save, and are no longer needed
     */
    private final ObjectSet<ChunkPos> queuedUnloads = new ObjectOpenHashSet<>();

    /**
     * The set of all deleted sub-levels, which need to be cleared from their holding chunks and their data storages
     */
    private final ObjectSet<GlobalSavedSubLevelPointer> queuedDeletion = new ObjectOpenHashSet<>();

    /**
     * The set of all chunk positions that have unloaded in the previous tick
     */
    private final LongSet chunksToUnload = new LongOpenHashSet();

    /**
     * The set of all chunk positions that have loaded in the previous tick
     */
    private final LongSet chunksToLoad = new LongOpenHashSet();

    public SubLevelHoldingChunkMap(final ServerLevel level, final ServerSubLevelContainer container) {
        this.level = level;
        this.container = container;

        final File worldFolder = level.getChunkSource().getDataStorage().dataFolder.getParentFile();
        final File subLevelsFolder = new File(worldFolder, "sublevels");

        subLevelsFolder.mkdirs();

        this.storage = new SubLevelStorage(subLevelsFolder.toPath());
    }

    public void updateChunkStatus(final ChunkPos chunkPos, final boolean loaded) {
        final long key = chunkPos.toLong();

        if (!loaded) {
            this.chunksToUnload.add(key);
            this.chunksToLoad.remove(key);
        } else {
            this.chunksToLoad.add(key);
            this.chunksToUnload.remove(key);
        }
    }

    private void processLoad(final ChunkPos chunkPos) {
        if (VERBOSE) {
            Sable.LOGGER.info("Processing load of chunk at {}", chunkPos);
        }

        if (this.queuedUnloads.contains(chunkPos)) {
            if (VERBOSE) {
                Sable.LOGGER.info("Removing chunk at {} from queued unloads", chunkPos);
            }
            this.queuedUnloads.remove(chunkPos);
        }

        if (this.loadedHoldingChunks.containsKey(chunkPos.toLong())) {
            return;
        }

        // when the chunk is loaded, we have to also load the holding chunk if it exists
        this.getOrLoadHoldingChunk(chunkPos, false);
    }

    private void processUnload(final ChunkPos chunkPos) {
        if (!this.loadedHoldingChunks.containsKey(chunkPos.toLong())) {
            return;
        }

        if (VERBOSE) {
            Sable.LOGGER.info("Processing unload for chunk {}", chunkPos);
        }

        final BoundingBox3d bounds = new BoundingBox3d(chunkPos.x << 4, -Double.MAX_VALUE, chunkPos.z << 4, (chunkPos.x << 4) + 16, Double.MAX_VALUE, (chunkPos.z << 4) + 16);

        final SubLevelContainer container = SubLevelContainer.getContainer(this.level);
        assert container != null : "Sub-level container is null";

        final Iterable<SubLevel> toUnloadIterator = container.queryIntersecting(bounds);
        final ObjectOpenHashSet<SubLevel> toUnload = new ObjectOpenHashSet<>();
        for (final SubLevel subLevel : toUnloadIterator) {
            toUnload.add(subLevel);
        }

        if (VERBOSE) {
            Sable.LOGGER.info("Adding chunk {} to queued unloads", chunkPos);
        }
        this.queuedUnloads.add(chunkPos);

        if (toUnload.isEmpty()) {
            return;
        }

        final SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(chunkPos, true);
        final ObjectSet<SubLevel> visited = new ObjectOpenHashSet<>();

        for (final SubLevel subLevel : toUnload) {
            if (visited.contains(subLevel)) {
                continue;
            }
            final ServerSubLevel serverSubLevel = (ServerSubLevel) subLevel;

            final Collection<ServerSubLevel> chain = SubLevelHelper.getLoadingDependencyChain(serverSubLevel);
            visited.addAll(chain);

            final List<UUID> uuids = chain.stream().map(SubLevel::getUniqueId).toList();

            for (final ServerSubLevel chainedSubLevel : chain) {
                final GlobalSavedSubLevelPointer pointer = chainedSubLevel.getLastSerializationPointer();

                if (VERBOSE) {
                    Sable.LOGGER.info("Unloading sub-level {} with pointer {} to chunk {} as holding sub-level", chainedSubLevel, pointer, chunkPos);
                }

                final SubLevelData data = SubLevelSerializer.toData(chainedSubLevel, uuids);
                final HoldingSubLevel holdingSubLevel = new HoldingSubLevel(data, pointer);
                holdingChunk.acceptHoldingSubLevel(holdingSubLevel);
                this.allHoldingSubLevels.put(holdingSubLevel.data().uuid(), holdingSubLevel);

                container.removeSubLevel(chainedSubLevel, SubLevelRemovalReason.UNLOADED);
            }
        }
    }

    /**
     * Saves the whole holding chunk map to disk.
     */
    public void saveAll() {
        if (VERBOSE) {
            Sable.LOGGER.info("Saving holding chunk-map");
        }

        this.processChanges();

        for (final GlobalSavedSubLevelPointer deletion : this.queuedDeletion) {
            if (VERBOSE) {
                Sable.LOGGER.info("Processing queued deletion & clearing data for {}", deletion);
            }
            this.storage.attemptSaveSubLevel(deletion, null);
        }
        this.queuedDeletion.clear();

        final List<ServerSubLevel> subLevels = this.container.getAllSubLevels();
        final Collection<ServerSubLevel> toMove = new ObjectArrayList<>(subLevels);
        final Collection<ServerSubLevel> moved = new ObjectArraySet<>(toMove.size());

        for (final ServerSubLevel subLevel : toMove) {
            if (moved.contains(subLevel)) {
                continue;
            }

            // We save all intersecting sub-levels in the chain to the first one's chunk
            final Vector3d currentPosition = subLevel.logicalPose().position();
            final ChunkPos moveToChunk = new ChunkPos(BlockPos.containing(currentPosition.x, currentPosition.y, currentPosition.z));

            final Collection<ServerSubLevel> chain = SubLevelHelper.getLoadingDependencyChain(subLevel);
            moved.addAll(chain);

            final List<UUID> uuids = chain.stream().map(SubLevel::getUniqueId).toList();
            for (final ServerSubLevel chainedSubLevel : chain) {
                if (VERBOSE) {
                    Sable.LOGGER.info("Moving sub-level {} with last pointer {}", chainedSubLevel, chainedSubLevel.getLastSerializationPointer());
                }
                this.moveAndSaveSubLevel(chainedSubLevel, moveToChunk, uuids);
            }
        }

        for (final SubLevelHoldingChunk holdingChunk : this.loadedHoldingChunks.values()) {
            final ChunkPos holdingChunkPos = holdingChunk.getChunkPos();

            for (final HoldingSubLevel holdingSubLevel : holdingChunk.getLoadedHoldingSubLevels()) {
                if (VERBOSE) {
                    Sable.LOGGER.info("Processing holding sub-level {} stored in chunk {} with pointer {}", holdingSubLevel, holdingChunkPos, holdingSubLevel.pointer());
                }

                if (holdingSubLevel.pointer() == null || !Objects.equals(holdingSubLevel.pointer().chunkPos(), holdingChunkPos)) {
                    if (VERBOSE) {
                        Sable.LOGGER.info("Chunk position of holding chunk and pointer mis-match. Moving");
                    }
                    final GlobalSavedSubLevelPointer newPointer = this.moveAndSaveSubLevel(null, holdingSubLevel.data(), holdingSubLevel.pointer(), holdingChunkPos);
                    holdingSubLevel.setPointer(newPointer);
                } else {
                    this.storage.attemptSaveSubLevel(holdingSubLevel.pointer(), holdingSubLevel.data());
                }
            }
        }

        for (final ChunkPos unload : this.queuedUnloads) {
            final SubLevelHoldingChunk holdingChunk = this.loadedHoldingChunks.get(unload.toLong());

            if (VERBOSE) {
                Sable.LOGGER.info("Processing queued unload for chunk {} at position {}", holdingChunk, holdingChunk != null ? holdingChunk.getChunkPos() : null);
            }

            if (holdingChunk != null) {
                for (final HoldingSubLevel holdingSubLevel : holdingChunk.getLoadedHoldingSubLevels()) {
                    this.allHoldingSubLevels.remove(holdingSubLevel.data().uuid());
                }
                this.setDirty(unload);
            }
        }

        for (final long longKey : this.dirtyHoldingChunks) {
            final ChunkPos chunkPos = new ChunkPos(longKey);

            final SubLevelHoldingChunk holdingChunk = this.loadedHoldingChunks.get(longKey);

            if (VERBOSE) {
                Sable.LOGGER.info("Saving holding chunk {} at {}", holdingChunk, chunkPos);
            }

            if (holdingChunk != null) {
                this.storage.attemptSaveHoldingChunk(chunkPos, holdingChunk);
            }
        }

        for (final ChunkPos unload : this.queuedUnloads) {
            this.loadedHoldingChunks.remove(unload.toLong());
        }
        this.queuedUnloads.clear();

        try {
            if (VERBOSE) {
                Sable.LOGGER.info("Flushing storage");
            }

            this.storage.flush();
        } catch (final IOException e) {
            Sable.LOGGER.error("Failed to flush sub-level storage to disk", e);
        }
    }

    private void moveAndSaveSubLevel(final ServerSubLevel subLevel, final ChunkPos moveToChunk, final List<UUID> uuids) {
        final GlobalSavedSubLevelPointer lastPointer = subLevel.getLastSerializationPointer();
        final SubLevelData data = SubLevelSerializer.toData(subLevel, uuids);
        subLevel.setLastSerializationPointer(this.moveAndSaveSubLevel(subLevel, data, lastPointer, moveToChunk));

        if (VERBOSE) {
            Sable.LOGGER.info("Moved sub-level {}. {} -> {}", subLevel, lastPointer, subLevel.getLastSerializationPointer());
        }
    }

    /**
     * Removes sub-level data from a previous pointer, and moves it to a new one.
     *
     * @param data        the data to move
     * @param lastPointer the previous pointer of the data
     * @param moveToChunk the chunk to move to
     * @return the new pointer
     */
    private GlobalSavedSubLevelPointer moveAndSaveSubLevel(final @Nullable ServerSubLevel subLevel, final SubLevelData data, final GlobalSavedSubLevelPointer lastPointer, final ChunkPos moveToChunk) {
        final ChunkPos oldChunkPos = lastPointer != null ? lastPointer.chunkPos() : null;

        if (Objects.equals(oldChunkPos, moveToChunk)) {
            if (this.getOrLoadHoldingChunk(moveToChunk, false) == null) {
                throw new IllegalStateException("this shouldn't be possible");
            }

            if (VERBOSE) {
                Sable.LOGGER.info("Old chunk is the same as the new chunk position ({}, {})", oldChunkPos, moveToChunk);
                Sable.LOGGER.info("Saving sub-level data to {}", lastPointer);
            }

            // re-save the data in-case the sub-level changed
            this.storage.attemptSaveSubLevel(lastPointer, data);
            this.setDirty(moveToChunk);
            return lastPointer;
        } else {
            if (VERBOSE) {
                Sable.LOGGER.info("Saving sub-level data to storage in new chunk, {}", moveToChunk);
            }
            // we moved chunks! remove us from the old one and save to the new one
            final GlobalSavedSubLevelPointer newPointer = this.storage.attemptSaveSubLevel(moveToChunk, data);

            if (newPointer == null) {
                final MinecraftServer server = this.level.getServer();
                if (server instanceof final SableToastableServer toastable) {
                    toastable.sable$reportSubLevelSaveFailure(data);
                    return null;
                }
            }

            if (VERBOSE) {
                Sable.LOGGER.info("New pointer {}", newPointer);
            }

            // move all the tracking points
            final SubLevelTrackingPointSavedData trackingPoints = SubLevelTrackingPointSavedData.getOrLoad(this.level);
            for (final Map.Entry<UUID, TrackingPoint> entry : trackingPoints.getAllTrackingPoints()) {
                final TrackingPoint point = entry.getValue();
                if (!point.inSubLevel()) {
                    continue;
                }

                final boolean movingPointers = point.lastSavedSubLevelPointer() != null && point.lastSavedSubLevelPointer().equals(lastPointer);
                final boolean pointerInSubLevel = subLevel != null && Sable.HELPER.getContaining(this.level, point.point()) == subLevel;

                if (movingPointers || pointerInSubLevel) {
                    trackingPoints.setTrackingPoint(entry.getKey(), new TrackingPoint(
                            true, point.subLevelID(), newPointer, point.point(), null
                    ));
                }
            }

            if (VERBOSE) {
                Sable.LOGGER.info("Clearing last pointer (if exists) {}", lastPointer);
            }

            if (lastPointer != null) {
                this.storage.attemptSaveSubLevel(lastPointer, null);
            }

            if (oldChunkPos != null) {
                final SavedSubLevelPointer localPointer = lastPointer.local();
                final SubLevelHoldingChunk oldHoldingChunk = this.getOrLoadHoldingChunk(oldChunkPos, false);

                if (VERBOSE) {
                    Sable.LOGGER.info("Removing pointer from last holding chunk {}", oldHoldingChunk);
                }

                if (oldHoldingChunk != null) {
                    // remove the sub-level from the old holding chunk
                    oldHoldingChunk.getSubLevelPointers().remove(localPointer);
                    this.setDirty(oldChunkPos);
                } else {
                    if (VERBOSE) {
                        Sable.LOGGER.info("Old holding chunk doesn't exist at {}! This may be a problem", oldChunkPos);
                    }
                }
            }

            final SubLevelHoldingChunk newHoldingChunk = this.getOrLoadHoldingChunk(moveToChunk, true);

            if (VERBOSE) {
                Sable.LOGGER.info("Adding pointer to new holding chunk.");
            }

            // add the sub-level to the new holding chunk
            final SavedSubLevelPointer newLocalPointer = newPointer.local();
            newHoldingChunk.getSubLevelPointers().add(newLocalPointer);
            this.setDirty(moveToChunk);

            return newPointer;
        }
    }

    /**
     * Gets, or loads a holding chunk for the given chunk position.
     *
     * @param chunkPos the chunk position to get or load the holding chunk for
     * @param create   whether to create a new holding chunk if it doesn't exist
     */
    @Contract("_, true -> !null")
    private SubLevelHoldingChunk getOrLoadHoldingChunk(final ChunkPos chunkPos, final boolean create) {
        final long longKey = chunkPos.toLong();
        final SubLevelHoldingChunk holdingChunk = this.loadedHoldingChunks.get(longKey);

        if (holdingChunk != null) {
            return holdingChunk; // already loaded
        }

        // try to load the holding chunk from disk
        final SubLevelHoldingChunk loadedChunk = this.storage.attemptLoadHoldingChunk(chunkPos);
        if (loadedChunk != null) {
            if (VERBOSE) {
                Sable.LOGGER.info("Loaded chunk at {} from disk", chunkPos);
            }

            final List<SavedSubLevelPointer> pointerQueue = loadedChunk.getSubLevelPointers();
            for (final SavedSubLevelPointer pointer : pointerQueue) {
                if (VERBOSE) {
                    Sable.LOGGER.info("Attempting to read pointer at {} into sub-level data", pointer);
                }

                final SubLevelData subLevelData = this.storage.attemptLoadSubLevel(chunkPos, pointer);

                if (subLevelData == null) {
                    Sable.LOGGER.error("Due to a failed storage sub-level data load, we can't add a holding sub-level for pointer {}. This will cause issues later down the line.", pointer);
                    continue;
                }

                final GlobalSavedSubLevelPointer globalPointer = new GlobalSavedSubLevelPointer(chunkPos, pointer.storageIndex(), pointer.subLevelIndex());

                final HoldingSubLevel holdingSubLevel = new HoldingSubLevel(subLevelData, globalPointer);
                loadedChunk.acceptHoldingSubLevel(holdingSubLevel);
                this.allHoldingSubLevels.put(holdingSubLevel.data().uuid(), holdingSubLevel);
            }

            this.loadedHoldingChunks.put(longKey, loadedChunk);
            return loadedChunk;
        }

        if (create) {
            // create a new holding chunk if it doesn't exist
            final SubLevelHoldingChunk newHoldingChunk = new SubLevelHoldingChunk(chunkPos);
            this.loadedHoldingChunks.put(longKey, newHoldingChunk);
            return newHoldingChunk;
        }
        return null;
    }

    private void setDirty(final ChunkPos chunkPos) {
        if (VERBOSE) {
            Sable.LOGGER.info("Setting chunk at {} as dirty", chunkPos);
        }

        this.dirtyHoldingChunks.add(chunkPos.toLong());
    }

    /**
     * Ticks the holding chunk map, checking for sub-levels that are ready to be loaded.
     */
    public void processChanges() {
        this.processUnloads();

        final Object2ObjectMap<UUID, HoldingSubLevel> readySubLevels = new Object2ObjectOpenHashMap<>();

        for (final SubLevelHoldingChunk chunk : this.loadedHoldingChunks.values()) {
            if (this.queuedUnloads.contains(chunk.getChunkPos())) {
                continue;
            }
            chunk.collectReadySubLevels(this.level, readySubLevels);
        }

        for (final HoldingSubLevel holdingSubLevel : readySubLevels.values()) {
            if (VERBOSE) {
                Sable.LOGGER.info("Holding sub-level {} with pointer {} reportedly ready to load", holdingSubLevel, holdingSubLevel.pointer());
            }

            final ServerSubLevel subLevel = SubLevelSerializer.fullyLoad(this.level, holdingSubLevel.data());

            if (subLevel != null) {
                subLevel.setLastSerializationPointer(holdingSubLevel.pointer());
            } else {
                final MinecraftServer server = this.level.getServer();
                if (server instanceof final SableToastableServer toastable) {
                    toastable.sable$reportSubLevelLoadFailure(holdingSubLevel.pointer());
                }
                Sable.LOGGER.info("Failed to load holding sub-level {} with pointer {}. This is a problem.", holdingSubLevel, holdingSubLevel.pointer());
            }

            this.allHoldingSubLevels.remove(holdingSubLevel.data().uuid());
        }
    }

    /**
     * Queries a currently loaded holding-sub-level, waiting either to be added to the level or to be unloaded
     *
     * @param uuid the uuid to query
     * @return the holding sub-level, if one exists.
     */
    public @Nullable HoldingSubLevel getHoldingSubLevel(final UUID uuid) {
        return this.allHoldingSubLevels.get(uuid);
    }

    private void processUnloads() {
        for (final long l : this.chunksToUnload) {
            this.processUnload(new ChunkPos(l));
        }

        for (final long l : this.chunksToLoad) {
            this.processLoad(new ChunkPos(l));
        }
        this.chunksToUnload.clear();
        this.chunksToLoad.clear();
    }

    public void moveToUnloaded(final ServerSubLevel subLevel, final ChunkPos pos) {
        if (VERBOSE) {
            Sable.LOGGER.info("Sub-level {} with pointer {} detected unloaded chunk, moving to {}", subLevel, subLevel.getLastSerializationPointer(), pos);
        }
        final Collection<ServerSubLevel> chain = SubLevelHelper.getLoadingDependencyChain(subLevel);
        final List<UUID> uuids = chain.stream().map(SubLevel::getUniqueId).toList();

        final SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(pos, true);
        for (final ServerSubLevel chainSubLevel : chain) {
            final SubLevelData data = SubLevelSerializer.toData(chainSubLevel, uuids);

            final HoldingSubLevel holdingSubLevel = new HoldingSubLevel(data, chainSubLevel.getLastSerializationPointer());
            holdingChunk.acceptHoldingSubLevel(holdingSubLevel);
            this.allHoldingSubLevels.put(holdingSubLevel.data().uuid(), holdingSubLevel);

            if (VERBOSE) {
                Sable.LOGGER.info("Added {} to holding chunk {}", chainSubLevel, holdingChunk);
            }

            this.container.removeSubLevel(chainSubLevel, SubLevelRemovalReason.UNLOADED);
        }

        this.setDirty(pos);
    }

    public void queueDeletion(final ServerSubLevel subLevel) {
        final GlobalSavedSubLevelPointer pointer = subLevel.getLastSerializationPointer();

        if (VERBOSE) {
            Sable.LOGGER.info("Queuing sub-level {} with pointer {} for deletion", subLevel, pointer);
        }

        if (pointer != null) {
            final ChunkPos chunkPos = pointer.chunkPos();
            final SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(chunkPos, false);

            if (holdingChunk != null) {
                holdingChunk.getSubLevelPointers().remove(pointer.local());
                this.setDirty(chunkPos);
            }
            this.queuedDeletion.add(pointer);
        }
    }

    public SubLevelStorage getStorage() {
        return this.storage;
    }

    @Override
    public void close() throws Exception {
        this.storage.close();
    }
}
