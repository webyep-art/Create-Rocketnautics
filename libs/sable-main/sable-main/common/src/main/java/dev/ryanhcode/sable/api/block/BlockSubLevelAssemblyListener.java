package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An interface for sub-classes of {@link net.minecraft.world.level.block.Block} to implement that indicates the
 * {@link SubLevelAssemblyHelper} should notify the block any time it is "moved" as a part of sub-level assembly.
 */
public interface BlockSubLevelAssemblyListener {

    /**
     * Called before the {@link SubLevelAssemblyHelper} has moved a block of state newState from oldPos to newPos.
     *
     * @param originLevel the level the block will be moved from
     * @param resultingLevel the level the block will be moved to
     * @param newState the new block state
     * @param oldPos the old block position
     * @param newPos the new block position
     */
    default void beforeMove(final ServerLevel originLevel, final ServerLevel resultingLevel, final BlockState newState, final BlockPos oldPos, final BlockPos newPos) {

    }



    /**
     * Called after the {@link SubLevelAssemblyHelper} has moved a block of state newState from oldPos to newPos.
     * At this point in time during the move, the old block has not been removed.
     *
     * @param originLevel the level the block was moved from
     * @param resultingLevel the level the block was moved to
     * @param newState the new block state
     * @param oldPos the old block position
     * @param newPos the new block position
     */
    void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos);

}
