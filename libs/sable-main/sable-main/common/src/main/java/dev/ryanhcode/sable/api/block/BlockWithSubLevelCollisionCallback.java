package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for sub-classes of {@link net.minecraft.world.level.block.Block} to implement for physics collision callbacks.
 */
public interface BlockWithSubLevelCollisionCallback {

    /**
     * Gets the collision callback a given block state should have
     * @param state the block state to check
     * @return the block collision callback that should be used for that state
     */
    static BlockSubLevelCollisionCallback sable$getCallback(final BlockState state) {
         if (state.getBlock() instanceof final BlockWithSubLevelCollisionCallback blockCollisionCallback) {
             return blockCollisionCallback.sable$getCallback();
         }

         if (((BlockStateExtension) state).sable$getProperty(PhysicsBlockPropertyTypes.FRAGILE.get())) {
             return FragileBlockCallback.INSTANCE;
         }

         return null;
    }

    /**
     * Checks if a block state should have a collision callback used
     * @param state the block state to check
     * @return if the block state should have collision callbacks used
     */
    static boolean hasCallback(final BlockState state) {
        return sable$getCallback(state) != null;
    }

    BlockSubLevelCollisionCallback sable$getCallback();

}
