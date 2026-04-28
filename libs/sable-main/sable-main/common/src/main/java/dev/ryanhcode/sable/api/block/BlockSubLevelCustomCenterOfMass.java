package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3dc;

/**
 * Interface for sub-classes of {@link net.minecraft.world.level.block.Block} to implement to specify a custom center
 * of mass for sub-level physics.
 */
public interface BlockSubLevelCustomCenterOfMass {

    /**
     * Gets the center of mass that will be baked for a given block-state of this block.
     *
     * @param blockGetter the blockGetter to bake the center of mass for
     * @param state       the block state to bake the center of mass for
     * @return the center of mass relative to the lower corner of the block
     */
    Vector3dc getCenterOfMass(final BlockGetter blockGetter, final BlockState state);

}
