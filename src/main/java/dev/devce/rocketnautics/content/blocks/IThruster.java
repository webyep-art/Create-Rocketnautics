package dev.devce.rocketnautics.content.blocks;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IThruster {
    boolean isActive();
    boolean isRemoved();
    BlockPos getBlockPos();
    Level getLevel();
    ScrollValueBehaviour getThrustPower();
}
