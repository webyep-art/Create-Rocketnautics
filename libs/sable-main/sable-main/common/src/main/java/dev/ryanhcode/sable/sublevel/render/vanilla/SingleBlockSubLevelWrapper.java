package dev.ryanhcode.sable.sublevel.render.vanilla;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SingleBlockSubLevelWrapper implements BlockAndTintGetter {

    private ClientLevel level;
    private final BlockPos.MutableBlockPos globalPos;
    private final BlockPos.MutableBlockPos localPos;
    private BlockState state;

    public SingleBlockSubLevelWrapper() {
        this.globalPos = new BlockPos.MutableBlockPos();
        this.localPos = new BlockPos.MutableBlockPos();
    }

    public void setup(final ClientLevel level, final double x, final double y, final double z, final BlockPos localPos, final BlockState state) {
        this.level = level;
        this.globalPos.set(x, y, z);
        this.localPos.set(localPos);
        this.state = state;
    }

    public void clear() {
        this.level = null;
    }

    @Override
    public float getShade(final Direction direction, final boolean bl) {
        return this.level.getShade(direction, bl);
    }

    @Override
    public @NotNull LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public int getBrightness(final LightLayer lightLayer, final BlockPos pos) {
        return this.getLightEngine().getLayerListener(lightLayer).getLightValue(this.globalPos);
    }

    @Override
    public int getRawBrightness(final BlockPos pos, final int i) {
        return this.getLightEngine().getRawBrightness(this.globalPos, i);
    }

    @Override
    public boolean canSeeSky(final BlockPos pos) {
        return this.getBrightness(LightLayer.SKY, this.globalPos) >= this.getMaxLightLevel();
    }

    @Override
    public int getBlockTint(final BlockPos pos, final ColorResolver colorResolver) {
        return this.level.getBlockTint(pos, colorResolver);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
        return this.level.getBlockEntity(pos);
    }

    @Override
    public @NotNull BlockState getBlockState(final BlockPos pos) {
        if (pos.equals(this.localPos)) {
            return this.state;
        }

        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public @NotNull FluidState getFluidState(final BlockPos pos) {
        if (pos.equals(this.localPos)) {
            return this.state.getFluidState();
        }

        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    public ClientLevel getLevel() {
        return this.level;
    }
}
