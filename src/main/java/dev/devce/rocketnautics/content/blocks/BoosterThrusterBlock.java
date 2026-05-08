package dev.devce.rocketnautics.content.blocks;

import com.mojang.serialization.MapCodec;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BoosterThrusterBlock extends AbstractRocketThrusterBlock<BoosterThrusterBlockEntity> {
    public static final com.mojang.serialization.MapCodec<BoosterThrusterBlock> CODEC = simpleCodec(BoosterThrusterBlock::new);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

    public BoosterThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.UP).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
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
        // TODO attach this as a behavior in the block entity instead
        return type == RocketBlockEntities.BOOSTER_THRUSTER.get() ? (level1, pos, state1, blockEntity) -> BoosterThrusterBlockEntity.tick(level1, pos, state1, (BoosterThrusterBlockEntity) blockEntity) : null;
    }

    @Override
    public Class<BoosterThrusterBlockEntity> getBlockEntityClass() {
        return BoosterThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BoosterThrusterBlockEntity> getBlockEntityType() {
        return RocketBlockEntities.BOOSTER_THRUSTER.get();
    }
}
