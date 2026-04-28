package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.physics.collider.VoxelColliderData;

/**
 * An interface for sub-classes of {@link net.minecraft.world.level.block.Block} to implement that indicates they
 * have a dynamic collider. Dynamic colliders will be significantly more performance
 */
public interface BlockSubLevelDynamicCollider {

    void buildBoxes(VoxelColliderData data);

}
