package dev.devce.rocketnautics.content.blocks;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Solid fuel booster engine.
 * Consumes coal blocks to generate high thrust for a limited time.
 */
public class BoosterThrusterBlockEntity extends AbstractThrusterBlockEntity {

    private static final long FUEL_SCAN_CACHE_TICKS = 5L;
    private static final int TICKS_PER_FUEL_BLOCK = 200;
    private static final int MAX_SCAN_DISTANCE = 64;

    private ScrollValueBehaviour thrustPower;
    
    private boolean ignited = false;
    private boolean isSpent = false;
    private int fuelTicksRemaining = 0;
    
    private BlockPos cachedFuelPos = null;
    private long cachedFuelPosTick = Long.MIN_VALUE;
    private boolean fuelCacheValid = false;

    public BoosterThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.BOOSTER_THRUSTER.get(), pos, state);
    }

    @Override
    public ScrollValueBehaviour getThrustPower() {
        return thrustPower;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        thrustPower = new ScrollValueBehaviour(
                Component.translatable("gui.rocketnautics.thrust_power"), 
                this, 
                new CenteredSideValueBoxTransform((state, direction) -> direction != getThrustDirection())
        );
        thrustPower.between(1, 50);
        thrustPower.withFormatter(v -> (v * (int)THRUST_FORCE_MULTIPLIER) + " N");
        thrustPower.setValue(5);
        
        behaviours.add(thrustPower);
    }

    @Override
    public int getCurrentPower() {
        return isActive() ? thrustPower.getValue() : 0;
    }

    @Override
    public boolean isActive() {
        if (level == null) return false;
        if (level.isClientSide) return currentlyBurning;
        
        if (isSpent) return false;
        if (ignited && fuelTicksRemaining > 0) return true;

        if (!ignited) {
            return attemptIgnition();
        }
        
        return attemptFuelReload();
    }

    private boolean attemptIgnition() {
        boolean redstonePowered = getBlockState().getValue(BoosterThrusterBlock.POWERED);
        if (redstonePowered && hasSolidFuelBehind()) {
            ignited = true;
            consumeSolidFuelBlock();
            notifyUpdate();
            return true;
        }
        return false;
    }

    private boolean attemptFuelReload() {
        if (fuelTicksRemaining <= 0) {
            if (hasSolidFuelBehind()) {
                consumeSolidFuelBlock();
                notifyUpdate();
                return true;
            } else {
                isSpent = true;
                notifyUpdate();
                return false;
            }
        }
        return true;
    }

    @Override
    protected void updateActiveState() {
        if (level == null || level.isClientSide) return;

        boolean active = isActive();
        int oldFuel = fuelTicksRemaining;

        if (active != currentlyBurning) {
            currentlyBurning = active;
            sendData();
        }

        if (active && fuelTicksRemaining > 0) {
            fuelTicksRemaining--;
        }

        // Periodically sync fuel level to client for tooltip accuracy
        if (active && (level.getGameTime() % 20 == 0 || oldFuel != fuelTicksRemaining && fuelTicksRemaining % 100 == 0)) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }

    @Override
    protected void updateClientVisuals(boolean active) {
        super.updateClientVisuals(active);
        
        if (active) {
            spawnIgnitionParticles();
        }
    }

    private void spawnIgnitionParticles() {
        BlockPos fuelPos = findFuelPos();
        if (fuelPos != null) {
            for (int i = 0; i < 2; i++) {
                double fx = fuelPos.getX() + level.random.nextDouble();
                double fy = fuelPos.getY() + level.random.nextDouble();
                double fz = fuelPos.getZ() + level.random.nextDouble();
                level.addParticle(ParticleTypes.FLAME, fx, fy, fz, 0, 0, 0);
            }
        }
    }

    private boolean hasSolidFuelBehind() {
        return findFuelPos() != null;
    }

    private BlockPos findFuelPos() {
        if (level != null) {
            long gameTime = level.getGameTime();
            if (fuelCacheValid && gameTime - cachedFuelPosTick <= FUEL_SCAN_CACHE_TICKS) {
                return cachedFuelPos;
            }
        }

        BlockPos scannedFuelPos = scanForFuel();
        cachedFuelPos = scannedFuelPos;
        fuelCacheValid = true;
        cachedFuelPosTick = level != null ? level.getGameTime() : Long.MIN_VALUE;
        return scannedFuelPos;
    }

    private BlockPos scanForFuel() {
        Direction back = getThrustDirection().getOpposite();
        
        // Determine search axes perpendicular to the thrust direction
        Direction axis1, axis2;
        if (back.getAxis() == Direction.Axis.Y) {
            axis1 = Direction.NORTH;
            axis2 = Direction.EAST;
        } else if (back.getAxis() == Direction.Axis.X) {
            axis1 = Direction.UP;
            axis2 = Direction.NORTH;
        } else {
            axis1 = Direction.UP;
            axis2 = Direction.EAST;
        }

        BlockPos furthestFuel = null;

        for (int dist = 1; dist <= MAX_SCAN_DISTANCE; dist++) {
            BlockPos layerCenter = worldPosition.relative(back, dist);
            BlockPos foundInLayer = scanLayer(layerCenter, axis1, axis2);
            
            if (foundInLayer != null) {
                furthestFuel = foundInLayer;
            } else {
                break; // Chain broken
            }
        }
        
        return furthestFuel;
    }

    private BlockPos scanLayer(BlockPos center, Direction axis1, Direction axis2) {
        BlockPos lastFound = null;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos fuelPos = center.relative(axis1, i).relative(axis2, j);
                if (isBlockCombustible(level.getBlockState(fuelPos))) {
                    lastFound = fuelPos;
                }
            }
        }
        return lastFound;
    }

    private boolean isBlockCombustible(BlockState state) {
        return state.is(Blocks.COAL_BLOCK);
    }

    private void consumeSolidFuelBlock() {
        BlockPos fuelPos = findFuelPos();
        if (fuelPos != null) {
            level.setBlock(fuelPos, Blocks.AIR.defaultBlockState(), 3);
            fuelTicksRemaining = TICKS_PER_FUEL_BLOCK;
            invalidateFuelCache();
        }
    }

    private void invalidateFuelCache() {
        fuelCacheValid = false;
        cachedFuelPos = null;
        cachedFuelPosTick = Long.MIN_VALUE;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Ignited", ignited);
        tag.putBoolean("Spent", isSpent);
        tag.putInt("FuelTicks", fuelTicksRemaining);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        ignited = tag.getBoolean("Ignited");
        isSpent = tag.getBoolean("Spent");
        fuelTicksRemaining = tag.getInt("FuelTicks");
        invalidateFuelCache();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // We override the status line to include "Spent" state
        tooltip.add(Component.literal("    ").append(Component.translatable(getBlockState().getBlock().getDescriptionId()).withStyle(ChatFormatting.GOLD)));
        
        Component status;
        if (isActive()) {
            status = Component.translatable("rocketnautics.goggles.active").withStyle(ChatFormatting.GREEN);
        } else if (isSpent) {
            status = Component.translatable("rocketnautics.goggles.spent").withStyle(ChatFormatting.DARK_GRAY);
        } else {
            status = Component.translatable("rocketnautics.goggles.inactive").withStyle(ChatFormatting.RED);
        }
        
        tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.status")).append(": ").append(status));
        
        int power = thrustPower.getValue();
        tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.thrust")).append(": ")
                .append(Component.literal(power * (int)THRUST_FORCE_MULTIPLIER + " N").withStyle(ChatFormatting.GOLD)));
        
        if (ignited && fuelTicksRemaining > 0) {
            tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.burn_time")).append(": ")
                    .append(Component.literal((fuelTicksRemaining / 20) + "s").withStyle(ChatFormatting.AQUA)));
        }
        
        return true;
    }

    @Override
    public double getAvailableFuelMass() {
        // 1 Coal Block is 900 kg. We count the current block + any blocks behind it.
        int blocks = (fuelTicksRemaining > 0) ? 1 : 0;
        BlockPos nextFuel = scanForFuel();
        if (nextFuel != null) {
            // Very rough estimate: distance to furthest fuel block
            double dist = Math.sqrt(worldPosition.distSqr(nextFuel));
            blocks += (int) dist;
        }
        return blocks * 900.0;
    }

    @Override
    public double getFuelConsumptionPerTick() {
        return isActive() ? (900.0 / TICKS_PER_FUEL_BLOCK) : 0;
    }

    @Override
    public double getSpecificImpulse() {
        // Effective exhaust velocity for solid boosters ~2400 m/s
        return 2400.0;
    }
}
