package dev.ryanhcode.sable.physics.impl.rapier.collider;

import dev.ryanhcode.sable.api.physics.collider.VoxelColliderData;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import org.joml.Vector3dc;

/**
 * Represents a block physics data entry in the physics world.
 *
 * @param handle the internal integer handle of the block physics data entry
 */
public record RapierVoxelColliderData(int handle) implements VoxelColliderData {
    public static final RapierVoxelColliderData EMPTY = new RapierVoxelColliderData(-1);

    /**
     * Adds a collision box to the block physics data entry.
     * Coordinates are expected to be within a single voxel space of the block, 0-1.
     *
     * @param min the minimum corner of the box
     * @param max the maximum corner of the box
     */
    @Override
    public void addBox(final Vector3dc min, final Vector3dc max) {
        Rapier3D.addVoxelColliderBox(this.handle, new double[]{min.x(), min.y(), min.z(), max.x(), max.y(), max.z()});
    }

    /**
     * Clears all collision boxes
     */
    @Override
    public void clearBoxes() {
        Rapier3D.clearVoxelColliderBoxes(this.handle);
    }
}
