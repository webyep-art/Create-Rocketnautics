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
    
    /**
     * @return The current thrust power in Newtons (after scaling).
     */
    int getCurrentPower();
    
    /**
     * @return Estimated total mass of available fuel in kilograms.
     */
    double getAvailableFuelMass();

    /**
     * @return Current fuel consumption in kilograms per tick.
     */
    double getFuelConsumptionPerTick();

    /**
     * @return Specific impulse (effective exhaust velocity) in m/s.
     */
    double getSpecificImpulse();
}
