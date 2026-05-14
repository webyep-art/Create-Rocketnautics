package dev.devce.rocketnautics.compat.computercraft.generic;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import dev.devce.rocketnautics.content.blocks.*;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

import static dev.devce.rocketnautics.RocketNautics.MODID;

public class ThrusterMethods implements GenericPeripheral {
    @Override
    public @NonNull PeripheralType getType() {
        return PeripheralType.ofAdditional("thruster");
    }

    @Override
    public @NonNull String id() {
        return MODID + ":thruster";
    }

    private String getEngineType(IThruster thruster) {
        return switch (thruster) {
            case VectorThrusterBlockEntity v -> "vector_thruster";
            case RCSThrusterBlockEntity rcs -> "rcs_thruster";
            case RocketThrusterBlockEntity r -> "rocket_thruster";
            case BoosterThrusterBlockEntity b -> "booster_thruster";
            default -> "generic_thruster" ;
        };
    }

    @LuaFunction(mainThread = true)
    public final boolean isActive(IThruster thruster) {
        return thruster.isActive();
    }

    @LuaFunction(mainThread = true)
    public final void setActive(IThruster thruster, boolean active) {
        thruster.setActive(active);
    }

    @LuaFunction(mainThread = true)
    public final float getThrust(IThruster thruster) {
        return switch (thruster) {
            case RocketThrusterBlockEntity rt -> (float)rt.getCurrentPower() * 10.0F;
            case BoosterThrusterBlockEntity bt -> (float)bt.getThrustPower().getValue() * 10.0F;
            default -> 0F;
        };
    }

    @LuaFunction(mainThread = true)
    public final void setThrust(IThruster thruster, int power) {
        ScrollValueBehaviour behaviour = thruster.getThrustPower();
        if (behaviour != null)
            behaviour.setValue(Math.max(0, Math.min(50, power / 10)));
    }

    @LuaFunction(
        mainThread = true
    )
    public final Map<String, Object> getData(IThruster thruster) {
        Map<String, Object> data = new HashMap<>();
        data.put("active", thruster.isActive());
        data.put("engine_type", getEngineType(thruster));
        switch (thruster) {
            case RocketThrusterBlockEntity rt -> {
                data.put("throttle", rt.fuelThrottle);
                data.put("fuel_usage", rt.currentFuelUsage);
                data.put("fuel_amount", rt.fuelTank.getFluidAmount());
                data.put("fuel_capacity", rt.fuelTank.getCapacity());
                data.put("fuel_name", rt.fuelTank.getFluid().getHoverName().getString());
                data.put("ignition_ticks", rt.ignitionTicks);
                data.put("warmup_time", rt.getWarmupTime());
                data.put("isp_multiplier", rt.getCurrentIspMultiplier());
                data.put("efficiency_multiplier", rt.getCurrentEfficiencyMultiplier());
                data.put("is_steam_mode", rt.isSteamMode());
                data.put("startup_ticks", rt.getStartupTicks());
                data.put("burnout_delay", rt.getBurnoutDelay());
                if (rt instanceof VectorThrusterBlockEntity vt) {
                    Map<String, Float> gimbal = new HashMap<>();
                    gimbal.put("x", vt.getGimbalX());
                    gimbal.put("y", vt.getGimbalY());
                    gimbal.put("z", vt.getGimbalZ());
                    data.put("gimbal", gimbal);
                }
            }
            case BoosterThrusterBlockEntity bt -> {
                data.put("ignition_ticks", bt.ignitionTicks);
                data.put("thrust_power", bt.getThrustPower().getValue() * 10);
                data.put("ignited", bt.isIgnited());
                data.put("is_spent", bt.isSpent());
                data.put("fuel_ticks", bt.getFuelTicks());
            }
            default -> {}
        }
        return data;
    }

    @LuaFunction(mainThread = true)
    public final float getFuelThrottle(RocketThrusterBlockEntity thruster) {
        return thruster.fuelThrottle;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Float> getGimbal(VectorThrusterBlockEntity thruster) {
        Map<String, Float> gimbal = new HashMap<>();
        gimbal.put("x", thruster.getGimbalX());
        gimbal.put("y", thruster.getGimbalY());
        gimbal.put("z", thruster.getGimbalZ());
        return gimbal;

    }

    @LuaFunction(mainThread = true)
    public final void setGimbal(VectorThrusterBlockEntity thruster, double x, double y, double z) {
        thruster.setComputerGimbal((float)x, (float)y, (float)z);
    }
}
