package dev.devce.rocketnautics.api.nodes;

import dev.devce.rocketnautics.api.peripherals.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.UUID;

public interface NodeContext {
    double getAltitude();
    double getVelocity();
    double getPitch();
    double getYaw();
    double getRoll();
    double getX();
    double getY();
    double getZ();
    
    Level getLevel();
    BlockPos getBlockPos();
    
    List<IPeripheral> getPeripherals();
    void setOutput(String side, int strength);

    double evaluateInput(UUID nodeId, int pin);
}
