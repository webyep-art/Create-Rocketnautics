package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BoosterThrusterBlock extends RocketThrusterBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

    public BoosterThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.UP).setValue(POWERED, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED, LIT);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
                .setValue(LIT, false);
    }

    @Override
    public void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean isPowered = level.hasNeighborSignal(pos);
            if (isPowered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, isPowered), 3);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == RocketBlockEntities.BOOSTER_THRUSTER.get() ? (level1, pos, state1, blockEntity) -> AbstractThrusterBlockEntity.tick(level1, pos, state1, (AbstractThrusterBlockEntity) blockEntity) : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return RocketBlockEntities.BOOSTER_THRUSTER.get().create(pos, state);
    }
}
