package dev.devce.rocketnautics.content.blocks.rope;

import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MultiRopeHubBlock extends RopeConnectorBlock {

    public MultiRopeHubBlock(Properties properties) {
        super(properties);
    }

    // ── Block Entity ──────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiRopeHubBlockEntity(RocketBlockEntities.MULTI_ROPE_HUB.get(), pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == RocketBlockEntities.MULTI_ROPE_HUB.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<MultiRopeHubBlockEntity>)
                    (lvl, pos, blockState, be) -> be.tick();
        }
        return null;
    }

    // ── Shearing support ──────────────────────────────────────────────────────

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (stack.getItem() instanceof net.minecraft.world.item.ShearsItem) {
                if (level.getBlockEntity(pos) instanceof MultiRopeHubBlockEntity be) {
                    return be.shearLastRope(sp, pos);
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    // ── Movement (called by Sable/Simulated after the ship moves) ─────────────

    public void onAfterMoved(ServerLevel level, BlockPos oldPos, BlockPos newPos, BlockState state) {
        if (level.getBlockEntity(newPos) instanceof MultiRopeHubBlockEntity be) {
            be.handleAfterMove(level, oldPos, newPos);
        }
    }
}
