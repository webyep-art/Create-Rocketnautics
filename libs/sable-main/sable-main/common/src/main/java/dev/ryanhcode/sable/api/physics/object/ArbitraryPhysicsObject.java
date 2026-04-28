package dev.ryanhcode.sable.api.physics.object;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.ChunkPos;

/**
 * An arbitrary physics object in a {@link dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem}
 */
public interface ArbitraryPhysicsObject {

    /**
     * Gathers the bounding box that the arbitrary physics object requires to be loaded.
     * Chunk sections intersecting this bounding box will be synced to the physics engine.
     *
     * @param dest the destination the bounding box should be written into
     */
    void getBoundingBox(final BoundingBox3d dest);

    /**
     * Called upon the physics object entering unloaded chunks
     */
    void onUnloaded(SubLevelHoldingChunkMap holdingChunkMap, ChunkPos chunkPos);

    /**
     * Called upon the physics object being removed from the system through means other than unloading
     */
    void onRemoved();

    /**
     * Called upon the physics object being added to the world
     */
    void onAddition(final SubLevelPhysicsSystem physicsSystem);

    /**
     * Called to wake up the physics object when nearby blocks were modified
     */
    void wakeUp();

}
