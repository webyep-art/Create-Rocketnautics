package dev.devce.rocketnautics.content.blocks;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import dev.ryanhcode.sable.Sable;

import java.util.List;

/**
 * Liquid fuel rocket engine.
 * Robust integration with TFMG and Conventional tags.
 */
public class RocketThrusterBlockEntity extends AbstractThrusterBlockEntity {

    private static final int TANK_CAPACITY = 1000;
    private static final int MAX_FUEL_CONSUMPTION = 40;  // mB/tick = 800 mB/s max
    private static final int STEAM_MIN_FLOW  = 2;         // 40 mB/s  — pre-ignition steam
    private static final int IGNITION_FLOW   = 5;         // 100 mB/s — full plasma flame
    private static final float MIN_THROTTLE_CHANGE = 0.01f;

    private final FluidTank fuelTank = new FluidTank(TANK_CAPACITY, this::isRocketFuel);
    private ScrollValueBehaviour minThrust;
    private ScrollValueBehaviour maxThrust;

    private float fuelThrottle = 0.0f;
    private float internalFlow = 0.0f;
    private int startupTicks = 0; // Timer for the 2-second steam phase
    private int currentFuelUsage = 0;
    private int totalAvailableFuel = 0;
    private float currentIspMultiplier = 1.0f;
    private float currentEfficiencyMultiplier = 1.0f;
    private int burnoutDelay = 0;
    private boolean steamMode = false;

    public RocketThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.ROCKET_THRUSTER.get(), pos, state);
    }

    protected RocketThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean isRocketFuel(FluidStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getFluid().isSame(Fluids.LAVA)) return true;
        if (stack.is(RocketTags.Fluids.ROCKET_FUEL)) return true;
        
        String id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
        return id.contains("tfmg") && (id.contains("kerosene") || id.contains("diesel") || id.contains("gasoline") || id.contains("fuel_oil") || id.contains("lpg"));
    }

    @Override
    public ScrollValueBehaviour getThrustPower() {
        return maxThrust;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        minThrust = new ScrollValueBehaviour(Component.translatable("gui.rocketnautics.min_thrust"), this, new CenteredSideValueBoxTransform((state, direction) -> direction != getThrustDirection()));
        minThrust.between(0, 50);
        minThrust.withFormatter(v -> (v * (int)THRUST_FORCE_MULTIPLIER) + " N");
        minThrust.setValue(0);
        
        maxThrust = new ScrollValueBehaviour(Component.translatable("gui.rocketnautics.max_thrust"), this, new CenteredSideValueBoxTransform((state, direction) -> direction != getThrustDirection()));
        maxThrust.between(0, 50);
        maxThrust.withFormatter(v -> (v * (int)THRUST_FORCE_MULTIPLIER) + " N");
        maxThrust.setValue(10);
        
        behaviours.add(minThrust);
        behaviours.add(maxThrust);
    }

    @Override
    public int getCurrentPower() {
        if (!isActive()) return 0;
        int min = minThrust.getValue();
        int max = maxThrust.getValue();
        if (min > max) min = max;
        return (int) (min + (max - min) * fuelThrottle);
    }

    @Override
    protected void updateActiveState() {
        if (level == null || level.isClientSide) return;

        boolean wasBurning = this.currentlyBurning;
        float oldThrottle = this.fuelThrottle;
        int oldUsage = this.currentFuelUsage;
        int oldFuel = this.totalAvailableFuel;

        updateFuelProperties();
        
        int targetConsumption = (int) (MAX_FUEL_CONSUMPTION * currentEfficiencyMultiplier);
        int actuallyDrained = attemptFuelDrain(targetConsumption);

        // Target flow based on actual fuel availability
        float targetFlow = actuallyDrained / (float) Math.max(1, targetConsumption);
        
        // TWO-PHASE STARTUP LOGIC:
        if (targetFlow > 0) {
            if (startupTicks < 10) { // 0.5 seconds of steam
                startupTicks++;
                // Phase 1: Steam phase (capped flow)
                float steamCap = (IGNITION_FLOW - 1.0f) / (float)MAX_FUEL_CONSUMPTION;
                this.internalFlow = Mth.lerp(0.2f, this.internalFlow, Math.min(targetFlow, steamCap));
            } else {
                // Phase 2: Ultra-fast expansion (Ignition)
                this.internalFlow = Mth.lerp(0.5f, this.internalFlow, targetFlow);
            }
        } else {
            startupTicks = 0;
            // Phase 3: Smooth shutdown (softer disappearance)
            this.internalFlow = Mth.lerp(0.05f, this.internalFlow, 0.00f);
            if (this.internalFlow < 0.001f) this.internalFlow = 0;
        }

        this.fuelThrottle = internalFlow;

        // Effective flow for logic
        float effectiveFlow = internalFlow * MAX_FUEL_CONSUMPTION;

        if (effectiveFlow >= (IGNITION_FLOW - 0.5f) && startupTicks >= 10) {
            this.currentlyBurning = true;
            this.steamMode = false;
            this.burnoutDelay = 10;
        } else if (effectiveFlow >= STEAM_MIN_FLOW || (targetFlow > 0 && startupTicks < 10)) {
            this.currentlyBurning = false;
            this.steamMode = true;
            this.burnoutDelay = 0;
        } else {
            if (burnoutDelay > 0) {
                burnoutDelay--;
            } else {
                this.currentlyBurning = false;
                this.steamMode = false;
            }
        }

        this.currentFuelUsage = actuallyDrained;
        this.totalAvailableFuel = fuelTank.getFluidAmount();
        
        if (hasStateChanged(wasBurning, oldThrottle, oldUsage, oldFuel) || level.getGameTime() % 20 == 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }

    private void updateFuelProperties() {
        FluidStack stack = fuelTank.getFluid();
        if (stack.isEmpty()) {
            currentIspMultiplier = 1.0f;
            currentEfficiencyMultiplier = 1.0f;
            return;
        }

        String fluidId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().toLowerCase();
        
        if (fluidId.contains("kerosene")) {
            currentIspMultiplier = 1.4f;      
            currentEfficiencyMultiplier = 0.5f; 
        } else if (fluidId.contains("diesel") || fluidId.contains("fuel_oil") || fluidId.contains("lpg")) {
            currentIspMultiplier = 1.2f;
            currentEfficiencyMultiplier = 0.7f;
        } else if (fluidId.contains("gasoline") || fluidId.contains("petrol")) {
            currentIspMultiplier = 1.1f;
            currentEfficiencyMultiplier = 0.8f;
        } else {
            currentIspMultiplier = 1.0f;
            currentEfficiencyMultiplier = 1.0f;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void updateClientVisuals(boolean active) {
        boolean steam = isSteamMode();
        updateSound(active);
        if (active || steam) {
            spawnThrustParticles();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void spawnThrustParticles() {
        Level level = getLevel();
        if (level == null) return;

        boolean steam = isSteamMode();
        boolean active = isActive();
        if (!active && !steam) return;

        BlockPos pos = getBlockPos();
        org.joml.Vector3d pDir = getParticleDirection();
        RandomSource random = level.getRandom();
        float power = getFuelThrottle();

        // --- 1. NOZZLE PARTICLES (Steam/Heat) ---
        if (steam || (active && random.nextFloat() < 0.3f)) {
            double x = pos.getX() + 0.5 + pDir.x * 1.05;
            double y = pos.getY() + 0.5 + pDir.y * 1.05;
            double z = pos.getZ() + 0.5 + pDir.z * 1.05;
            
            var type = steam ? ParticleTypes.CLOUD : ParticleTypes.SMOKE;
            int count = steam ? 4 : 1;
            
            for (int i = 0; i < count; i++) {
                double speedX = pDir.x * (0.2 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.1;
                double speedY = pDir.y * (0.2 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.1;
                double speedZ = pDir.z * (0.2 + random.nextDouble() * 0.2) + (random.nextDouble() - 0.5) * 0.1;
                level.addParticle(type, x, y, z, speedX, speedY, speedZ);
            }
        }

        // --- 2. SMART GROUND SMOKE (Collision based) ---
        if (active && power > 0.1f) {
            spawnGroundImpactEffects(level, pos, pDir, power, random);
        }
    }



    @Override
    public float getRenderPower() {
        return getFuelThrottle();
    }

    private boolean hasStateChanged(boolean wasBurning, float oldThrottle, int oldUsage, int oldFuel) {
        return wasBurning != currentlyBurning || Math.abs(oldThrottle - fuelThrottle) > MIN_THROTTLE_CHANGE || oldUsage != currentFuelUsage || Math.abs(oldFuel - totalAvailableFuel) > 50;
    }

    private int attemptFuelDrain(int amount) {
        // Only burns from the internal tank — fuel must be actively pumped in by pipes
        if (fuelTank.getFluidAmount() > 0) {
            int toDrain = Math.min(fuelTank.getFluidAmount(), amount);
            return fuelTank.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
        return 0;
    }

    // Removed drainFromAdjacent — thruster no longer self-pulls from adjacent blocks.
    // Fuel must be pushed into the thruster via Create pipes or other fluid transport.

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("FuelThrottle", fuelThrottle);
        tag.putBoolean("SteamMode", steamMode);
        tag.putInt("StartupTicks", startupTicks); // Sync timer
        tag.putInt("FuelUsage", currentFuelUsage);
        tag.putInt("TotalFuel", totalAvailableFuel);
        tag.putFloat("IspMult", currentIspMultiplier);
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fuelThrottle = tag.getFloat("FuelThrottle");
        steamMode = tag.getBoolean("SteamMode");
        startupTicks = tag.getInt("StartupTicks");
        currentFuelUsage = tag.getInt("FuelUsage");
        totalAvailableFuel = tag.getInt("TotalFuel");
        currentIspMultiplier = tag.getFloat("IspMult");
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        // Flow rate in mB/s (20 ticks per second)
        int flowPerSecond = currentFuelUsage * 20;
        ChatFormatting flowColor;
        String flowStatus;
        if (flowPerSecond >= 100) {
            flowColor = ChatFormatting.GREEN;
            flowStatus = "Ignition";
        } else if (flowPerSecond >= 40) {
            flowColor = ChatFormatting.YELLOW;
            flowStatus = "Pre-ignition (steam)";
        } else {
            flowColor = ChatFormatting.RED;
            flowStatus = "Insufficient";
        }
        tooltip.add(Component.literal("  » Flow Rate: ")
                .append(Component.literal(flowPerSecond + " mB/s").withStyle(flowColor))
                .append(Component.literal(" (" + flowStatus + ")").withStyle(ChatFormatting.DARK_GRAY)));

        if (totalAvailableFuel > 0) {
            String fuelName = fuelTank.getFluid().getDisplayName().getString();
            tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.fuel"))
                    .append(": ").append(Component.literal(fuelName + " (" + totalAvailableFuel + " mB)").withStyle(ChatFormatting.AQUA)));
        }
        if (currentIspMultiplier > 1.01f) {
            tooltip.add(Component.literal("  Fuel Quality: ")
                    .append(Component.literal(String.format("+%.0f%% Efficiency", (currentIspMultiplier - 1.0f) * 100)).withStyle(ChatFormatting.GREEN)));
        }
        return true;
    }

    public boolean isSteamMode() { return steamMode; }

    @Override
    public double getAvailableFuelMass() { return totalAvailableFuel * 2.5; }
    @Override
    public double getFuelConsumptionPerTick() { return currentFuelUsage * 2.5; }
    @Override
    public double getSpecificImpulse() { return 3100.0 * currentIspMultiplier; }
    public Direction getThrustDirection() { return getBlockState().getValue(RocketThrusterBlock.FACING); }
    public FluidTank getFuelTank() { return fuelTank; }
    public float getFuelThrottle() { return fuelThrottle; }
}
