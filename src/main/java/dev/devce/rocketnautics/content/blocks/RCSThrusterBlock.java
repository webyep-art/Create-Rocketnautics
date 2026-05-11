package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RCSThrusterBlock extends AbstractRocketThrusterBlock<RCSThrusterBlockEntity> {
    public static final com.mojang.serialization.MapCodec<RCSThrusterBlock> CODEC = simpleCodec(RCSThrusterBlock::new);

    public RCSThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        // TODO attach this as a behavior in the block entity instead
        return type == RocketBlockEntities.RCS_THRUSTER.get() ? (level1, pos, state1, blockEntity) -> RCSThrusterBlockEntity.tick(level1, pos, state1, (RCSThrusterBlockEntity) blockEntity) : null;
    }

    protected static final VoxelShape UP_SHAPE = Block.box(6, 0, 6, 10, 12, 10);
    protected static final VoxelShape DOWN_SHAPE = Block.box(6, 4, 6, 10, 16, 10);
    protected static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 4, 10, 10, 16);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 0, 10, 10, 12);
    protected static final VoxelShape EAST_SHAPE = Block.box(0, 6, 6, 12, 10, 10);
    protected static final VoxelShape WEST_SHAPE = Block.box(4, 6, 6, 16, 10, 10);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
        };
    }

    @Override
    public Class<RCSThrusterBlockEntity> getBlockEntityClass() {
        return RCSThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RCSThrusterBlockEntity> getBlockEntityType() {
        return RocketBlockEntities.RCS_THRUSTER.get();
    }
}
