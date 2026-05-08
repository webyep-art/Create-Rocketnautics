package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VectorThrusterBlock extends AbstractRocketThrusterBlock<VectorThrusterBlockEntity> {
    public static final com.mojang.serialization.MapCodec<VectorThrusterBlock> CODEC = simpleCodec(VectorThrusterBlock::new);

    public VectorThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VectorThrusterBlockEntity vectorBE) {
                // TODO attach this as a behavior in the block entity instead
                vectorBE.updateGimbalAngles();
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        // TODO attach this as a behavior in the block entity instead
        return type == getBlockEntityType() ? (level1, pos, state1, blockEntity) -> VectorThrusterBlockEntity.tick(level1, pos, state1, (VectorThrusterBlockEntity) blockEntity) : null;
    }

    @Override
    public Class<VectorThrusterBlockEntity> getBlockEntityClass() {
        return VectorThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VectorThrusterBlockEntity> getBlockEntityType() {
        return RocketBlockEntities.VECTOR_THRUSTER.get();
    }
}
