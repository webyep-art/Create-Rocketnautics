package dev.devce.rocketnautics.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.List;

public class SeparatorBlock extends DirectionalBlock {
    public static final MapCodec<SeparatorBlock> CODEC = simpleCodec(SeparatorBlock::new);

    public SeparatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        BlockPos clickedPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        
        if (clickedState.getBlock() == this) {
            // Если мы кликаем по сепаратору, инвертируем его направление для нового блока
            facing = clickedState.getValue(FACING).getOpposite();
        }
        
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            if (level.hasNeighborSignal(pos)) {
                triggerChainReaction(level, pos);
            }
        }
    }

    private void triggerChainReaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SeparatorBlock)) return;

        // Удаляем блок перед рекурсией, чтобы избежать бесконечного цикла
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5f, 1.5f + level.random.nextFloat());

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 5; i++) {
                double px = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
                double py = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5);
                double pz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0, 0, 0, 0.05);
                serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0, 0, 0.02);
            }
        }

        // Проверяем всех соседей (6 сторон)
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof SeparatorBlock) {
                // Небольшая задержка не нужна, мы хотим мгновенную реакцию
                triggerChainReaction(level, neighborPos);
            }
        }
    }


}
