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

/**
 * A block used to separate rocket stages.
 * When it receives a redstone signal, it destroys itself with an explosion effect.
 */
public class SeparatorBlock extends DirectionalBlock {

    public static final MapCodec<SeparatorBlock> CODEC = simpleCodec(SeparatorBlock::new);
    
    private static final int PARTICLE_COUNT = 15;
    private static final float EXPLOSION_VOLUME = 1.0f;
    private static final float EXPLOSION_PITCH = 1.5f;

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
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        
        if (level.hasNeighborSignal(pos)) {
            triggerSeparation(level, pos);
        }
    }

    private void triggerSeparation(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, EXPLOSION_VOLUME, EXPLOSION_PITCH);
        
        if (level instanceof ServerLevel serverLevel) {
            spawnSeparationParticles(serverLevel, pos);
        }
    }

    private void spawnSeparationParticles(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double px = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
            double py = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5);
            double pz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
            
            level.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0, 0, 0, 0.05);
            level.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0, 0, 0.02);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
