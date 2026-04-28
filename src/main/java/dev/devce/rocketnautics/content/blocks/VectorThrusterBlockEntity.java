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

/**
 * Gimbaled rocket engine.
 * Can tilt its thrust vector based on side signals.
 */
public class VectorThrusterBlockEntity extends RocketThrusterBlockEntity {

    private static final float GIMBAL_SENSITIVITY = 0.02f;
    private static final float GIMBAL_LERP_FACTOR = 0.2f;

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
        
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() != nozzle.getAxis()) {
                int signal = level.getSignal(worldPosition.relative(dir), dir);
                gX += dir.getStepX() * signal * GIMBAL_SENSITIVITY;
                gY += dir.getStepY() * signal * GIMBAL_SENSITIVITY;
                gZ += dir.getStepZ() * signal * GIMBAL_SENSITIVITY;
            }
        }
        
        gimbalX = gX;
        gimbalY = gY;
        gimbalZ = gZ;
        
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VectorThrusterBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.updateRenderGimbals();
        }
        
        RocketThrusterBlockEntity.tick(level, pos, state, blockEntity);
    }

    private void updateRenderGimbals() {
        renderGimbalX += (gimbalX - renderGimbalX) * GIMBAL_LERP_FACTOR;
        renderGimbalY += (gimbalY - renderGimbalY) * GIMBAL_LERP_FACTOR;
        renderGimbalZ += (gimbalZ - renderGimbalZ) * GIMBAL_LERP_FACTOR;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel serverSubLevel, RigidBodyHandle handle, double deltaTime) {
        if (!isActive()) return;

        Direction facing = getThrustDirection();
        Direction pushDirection = facing.getOpposite();
        double currentThrust = getCurrentPower() * THRUST_FORCE_MULTIPLIER;
        
        Vector3d thrustVector = new Vector3d(
                pushDirection.getStepX() + gimbalX,
                pushDirection.getStepY() + gimbalY,
                pushDirection.getStepZ() + gimbalZ
        ).normalize().mul(currentThrust);

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
