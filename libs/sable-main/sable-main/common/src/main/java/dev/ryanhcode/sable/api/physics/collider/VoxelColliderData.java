package dev.ryanhcode.sable.api.physics.collider;

import org.joml.Vector3dc;

public interface VoxelColliderData {
    /**
     * Adds a collision box to the block physics data entry.
     * Coordinates are expected to be within a single voxel space of the block, 0-1.
     *
     * @param min the minimum corner of the box
     * @param max the maximum corner of the box
     */
    void addBox(Vector3dc min, Vector3dc max);

    /**
     * Clears all collision boxes
     */
    void clearBoxes();
}
