package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public class VectorThrusterBlockEntity extends RocketThrusterBlockEntity {
    private static final Direction[] DIRECTIONS = Direction.values();

    private float gimbalX = 0;
    private float gimbalY = 0;
    private float gimbalZ = 0;
    
    private float renderGimbalX = 0;
    private float renderGimbalY = 0;
    private float renderGimbalZ = 0;

    public VectorThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.VECTOR_THRUSTER.get(), pos, state);
    }

    @Override
    public int getWarmupTime() {
        return 10;
    }

    @Override
    public Vector3d getParticleDirection() {
        Direction nozzle = getThrustDirection();
        return new Vector3d(
                nozzle.getStepX() - renderGimbalX,
                nozzle.getStepY() - renderGimbalY,
                nozzle.getStepZ() - renderGimbalZ
        ).normalize();
    }

    public void updateGimbalAngles() {
        if (level == null) return;
        Direction nozzle = getThrustDirection();
        
        float gX = 0;
        float gY = 0;
        float gZ = 0;
        
        for (Direction dir : DIRECTIONS) {
            if (dir.getAxis() != nozzle.getAxis()) {
                int signal = level.getSignal(worldPosition.relative(dir), dir);
                // Use a slightly larger multiplier for better range
                float strength = signal * 0.033f; // 15 * 0.033 ~= 0.5 (max tilt)
                gX += dir.getStepX() * strength;
                gY += dir.getStepY() * strength;
                gZ += dir.getStepZ() * strength;
            }
        }
        
        if (Math.abs(gimbalX - gX) > 0.001f || Math.abs(gimbalY - gY) > 0.001f || Math.abs(gimbalZ - gZ) > 0.001f) {
            gimbalX = gX;
            gimbalY = gY;
            gimbalZ = gZ;
            setChanged();
            // We use sendData() instead of full block update to save performance
            // sendData() is called by updateActiveState in parent tick if needed
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VectorThrusterBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.updateGimbalAngles();
        } else {
            blockEntity.renderGimbalX += (blockEntity.gimbalX - blockEntity.renderGimbalX) * 0.2f;
            blockEntity.renderGimbalY += (blockEntity.gimbalY - blockEntity.renderGimbalY) * 0.2f;
            blockEntity.renderGimbalZ += (blockEntity.gimbalZ - blockEntity.renderGimbalZ) * 0.2f;
        }
        
        RocketThrusterBlockEntity.tick(level, pos, state, blockEntity);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel serverSubLevel, RigidBodyHandle handle, double deltaTime) {
        if (!isActive()) return;

        Direction facing = getThrustDirection();
        Direction pushDirection = facing.getOpposite();
        
        // Ensure we have a base vector that isn't zero
        double vx = pushDirection.getStepX() + gimbalX;
        double vy = pushDirection.getStepY() + gimbalY;
        double vz = pushDirection.getStepZ() + gimbalZ;
        
        double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (length < 0.001) return; // Should not happen with base direction

        double currentThrust = getCurrentPower() * 10.0;
        Vector3d thrustVector = new Vector3d(vx / length, vy / length, vz / length).mul(currentThrust);

        Vector3d blockCenter = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        handle.applyImpulseAtPoint(blockCenter, thrustVector.mul(deltaTime));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("GimbalX", gimbalX);
        tag.putFloat("GimbalY", gimbalY);
        tag.putFloat("GimbalZ", gimbalZ);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        gimbalX = tag.getFloat("GimbalX");
        gimbalY = tag.getFloat("GimbalY");
        gimbalZ = tag.getFloat("GimbalZ");
    }

    public float getRenderGimbalX() { return renderGimbalX; }
    public float getRenderGimbalY() { return renderGimbalY; }
    public float getRenderGimbalZ() { return renderGimbalZ; }
}
