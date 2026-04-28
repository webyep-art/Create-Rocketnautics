package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Interface for sub-classes of {@link net.minecraft.world.level.block.Block} to implement to specify a separate
 * collision shape for sub-level physics.
 */
public interface BlockSubLevelCollisionShape {

    /**
     * Gets the collision shape that will be baked for a given block-state of this block.
     *
     * @param blockGetter the blockGetter to bake the collision shape for
     * @param state       the block state to bake the collision shape for
     * @return the collision shape that should be used for this block state
     */
    VoxelShape getSubLevelCollisionShape(final BlockGetter blockGetter, final BlockState state);

}
