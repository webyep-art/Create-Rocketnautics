package dev.devce.rocketnautics.content.blocks;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketParticles;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.List;

public class RCSThrusterBlockEntity extends RocketThrusterBlockEntity {

    public RCSThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.RCS_THRUSTER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
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
    public void sable$physicsTick(ServerSubLevel serverSubLevel, RigidBodyHandle handle, double deltaTime) {
        if (!isActive()) return;

        Direction facing = getThrustDirection();
        Direction pushDirection = facing.getOpposite();
        
        double currentThrust = 100.0;
        
        Vector3d thrustVector = new Vector3d(
                pushDirection.getStepX() * currentThrust,
                pushDirection.getStepY() * currentThrust,
                pushDirection.getStepZ() * currentThrust
        );

        Vector3d blockCenter = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        handle.applyImpulseAtPoint(blockCenter, thrustVector.mul(deltaTime));
    }

    @Override
    public int getWarmupTime() {
        return 0;
    }


    public static void tick(Level level, BlockPos pos, BlockState state, RCSThrusterBlockEntity blockEntity) {
        boolean active = blockEntity.isActive();
        if (!level.isClientSide) {
            if (active != blockEntity.currentlyBurning) {
                blockEntity.currentlyBurning = active;
                blockEntity.sendData();
            }
        }

        blockEntity.tick();
        
        if (level.isClientSide()) {
            if (active) {
                Direction nozzle = blockEntity.getThrustDirection();
                Vector3d pDir = new Vector3d(nozzle.getStepX(), nozzle.getStepY(), nozzle.getStepZ());
                
                double x = pos.getX() + 0.5 + pDir.x() * 0.55;
                double y = pos.getY() + 0.5 + pDir.y() * 0.55;
                double z = pos.getZ() + 0.5 + pDir.z() * 0.55;

                RandomSource random = level.getRandom();
                
                for (int i = 0; i < 2; i++) {
                    double speedX = pDir.x() * (0.3 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.05;
                    double speedY = pDir.y() * (0.3 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.05;
                    double speedZ = pDir.z() * (0.3 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.05;
                    
                    level.addParticle(RocketParticles.RCS_GAS.get(), x, y, z, speedX, speedY, speedZ);
                }
            }
        }
        
        if (active) {
            if (blockEntity.ignitionTicks < 10) blockEntity.ignitionTicks++;
        } else {
            if (blockEntity.ignitionTicks > 0) blockEntity.ignitionTicks--;
        }
    }

    @Override
    public void remove() {
        super.remove();
    }
}
