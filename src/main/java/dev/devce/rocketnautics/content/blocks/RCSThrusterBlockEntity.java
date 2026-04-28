package dev.devce.rocketnautics.content.blocks;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.List;

/**
 * Reaction Control System (RCS) Thruster.
 * Provides low thrust for orientation and fine movement.
 * Activated by redstone signal.
 */
public class RCSThrusterBlockEntity extends AbstractThrusterBlockEntity {

    private static final double RCS_THRUST_MAGNITUDE = 100.0;
    private static final int RCS_MAX_IGNITION_TICKS = 10;

    public RCSThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.RCS_THRUSTER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // RCS doesn't have scroll behaviours currently
    }

    @Override
    public ScrollValueBehaviour getThrustPower() {
        return null;
    }

    @Override
    public int getCurrentPower() {
        return isActive() ? 1 : 0;
    }

    @Override
    public boolean isActive() {
        if (level == null) return false;
        if (level.isClientSide) return currentlyBurning;
        return level.hasNeighborSignal(worldPosition);
    }

    @Override
    public int getWarmupTime() {
        return 0;
    }

    @Override
    protected void updateActiveState() {
        if (level == null || level.isClientSide) return;

        boolean active = isActive();
        if (active != currentlyBurning) {
            currentlyBurning = active;
            sendData();
        }
    }

    @Override
    protected void updateIgnition(boolean active) {
        if (active) {
            if (ignitionTicks < RCS_MAX_IGNITION_TICKS) ignitionTicks++;
        } else {
            if (ignitionTicks > 0) ignitionTicks--;
        }
    }

    @Override
    protected void spawnThrustParticles() {
        Direction nozzle = getThrustDirection();
        Vector3d pDir = new Vector3d(nozzle.getStepX(), nozzle.getStepY(), nozzle.getStepZ());
        
        double x = worldPosition.getX() + 0.5 + pDir.x() * 0.55;
        double y = worldPosition.getY() + 0.5 + pDir.y() * 0.55;
        double z = worldPosition.getZ() + 0.5 + pDir.z() * 0.55;

        RandomSource random = level.getRandom();
        
        for (int i = 0; i < 2; i++) {
            double speedX = pDir.x() * (0.3 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.05;
            double speedY = pDir.y() * (0.3 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.05;
            double speedZ = pDir.z() * (0.3 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.05;
            
            level.addParticle(RocketParticles.RCS_GAS.get(), x, y, z, speedX, speedY, speedZ);
        }
    }

    @Override
    protected Vector3d getPhysicsThrustVector(Direction pushDirection, double ignored) {
        // RCS has a fixed thrust magnitude
        return new Vector3d(
                pushDirection.getStepX() * RCS_THRUST_MAGNITUDE,
                pushDirection.getStepY() * RCS_THRUST_MAGNITUDE,
                pushDirection.getStepZ() * RCS_THRUST_MAGNITUDE
        );
    }

    @Override
    protected void applyThrusterEffects(net.minecraft.world.level.Level level, BlockPos pos) {
        // RCS is too weak to cause damage or block melting
    }

    @Override
    public double getAvailableFuelMass() {
        // Assume RCS uses some internal gas supply or atmosphere
        // For dV calculation, let's say it has a small virtual tank
        return isActive() ? 10.0 : 0.0;
    }

    @Override
    public double getFuelConsumptionPerTick() {
        return isActive() ? 0.1 : 0;
    }

    @Override
    public double getSpecificImpulse() {
        // Cold gas thrusters ~700 m/s
        return 700.0;
    }
}
