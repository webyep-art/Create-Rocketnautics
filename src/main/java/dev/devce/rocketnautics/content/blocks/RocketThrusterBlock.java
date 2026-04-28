package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class RocketThrusterBlock extends DirectionalBlock implements EntityBlock {
    public static final com.mojang.serialization.MapCodec<RocketThrusterBlock> CODEC = simpleCodec(RocketThrusterBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public RocketThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(LIT, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        // Redstone ignored for Rocket Thruster
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == RocketBlockEntities.ROCKET_THRUSTER.get() ? (level1, pos, state1, blockEntity) -> AbstractThrusterBlockEntity.tick(level1, pos, state1, (AbstractThrusterBlockEntity) blockEntity) : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return RocketBlockEntities.ROCKET_THRUSTER.get().create(pos, state);
    }
}
