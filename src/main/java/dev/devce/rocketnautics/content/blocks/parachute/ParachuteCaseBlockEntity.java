package dev.devce.rocketnautics.content.blocks.parachute;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.HolderLookup;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;

public class ParachuteCaseBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private ItemStack parachute = ItemStack.EMPTY;

    public ParachuteCaseBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.PARACHUTE_CASE.get(), pos, state);
    }

    public ItemStack getParachute() {
        return parachute;
    }

    public void setParachute(ItemStack stack) {
        this.parachute = stack;
        setChanged();
        if (level != null) {
            BlockState newState = getBlockState().setValue(ParachuteCaseBlock.HAS_PARACHUTE, !stack.isEmpty());
            level.setBlock(worldPosition, newState, 3);
        }
    }

    public boolean hasParachute() {
        return !parachute.isEmpty();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel serverSubLevel, RigidBodyHandle handle, double deltaTime) {
        BlockState state = getBlockState();
        if (state.hasProperty(ParachuteCaseBlock.OPEN) && state.getValue(ParachuteCaseBlock.OPEN)) {
            if (hasParachute()) {
                Vector3d globalVelocity = new Vector3d(handle.getLinearVelocity());
                if (globalVelocity.y() < -0.1) { // If falling even slightly
                    double dragStrength = 30.0; // Reduced drag for faster falling
                    double mass = serverSubLevel.getMassTracker().getMass();
                    
                    // Global drag forces
                    double downwardSpeed = -globalVelocity.y();
                    
                    // Cap the upward force so it doesn't launch the ship into the air
                    double upwardForce = Math.min(downwardSpeed * dragStrength * mass, mass * 15.0); 
                    
                    double horizontalDrag = dragStrength * 0.4 * mass;
                    
                    Vector3d globalDragImpulse = new Vector3d(
                        -globalVelocity.x() * horizontalDrag * deltaTime,
                        upwardForce * deltaTime,
                        -globalVelocity.z() * horizontalDrag * deltaTime
                    );
                    
                    // Convert global impulse to local ship coordinates
                    org.joml.Quaterniond orientation = serverSubLevel.logicalPose().orientation();
                    Vector3d localDragImpulse = orientation.transformInverse(globalDragImpulse, new Vector3d());
                    
                    // Apply force at the block's position to create stabilizing torque
                    Vector3d blockCenter = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
                    handle.applyImpulseAtPoint(blockCenter, localDragImpulse);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!parachute.isEmpty()) {
            tag.put("Parachute", parachute.saveOptional(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Parachute")) {
            parachute = ItemStack.parseOptional(registries, tag.getCompound("Parachute"));
        } else {
            parachute = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
