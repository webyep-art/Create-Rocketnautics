package dev.ryanhcode.sable.physics.impl;

import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public enum SableCollisionContextImpl implements SableCollisionContext {
    INSTANCE;

    @Override
    public boolean isDescending() {
        return false;
    }

    @Override
    public boolean isAbove(final VoxelShape shape, final BlockPos pos, final boolean canAscend) {
        return canAscend;
    }

    @Override
    public boolean isHoldingItem(final Item item) {
        return false;
    }

    @Override
    public boolean canStandOnFluid(final FluidState fluid1, final FluidState fluid2) {
        return false;
    }
}
