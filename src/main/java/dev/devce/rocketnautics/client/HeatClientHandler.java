package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketNautics;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RocketNautics.MODID, value = Dist.CLIENT)
public class HeatClientHandler {
    private static final Map<UUID, Float> HEAT_MAP = new HashMap<>();

    public static void updateHeat(UUID id, float intensity) {
        HEAT_MAP.put(id, intensity);
    }

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            HEAT_MAP.clear();
            return;
        }

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;

        HEAT_MAP.entrySet().removeIf(entry -> {
            UUID id = entry.getKey();
            float intensity = entry.getValue();

            SubLevel subLevel = container.getSubLevel(id);
            if (subLevel == null) return true;

            if (intensity > 0.05f) {
                spawnHeatParticles(level, subLevel, intensity);
                entry.setValue(intensity * 0.9f); // Gradual cooldown
            } else {
                return true;
            }
            return false;
        });
    }

    private static void spawnHeatParticles(ClientLevel level, SubLevel subLevel, float intensity) {
        AABB bounds = subLevel.getAABB();
        if (bounds == null) return;

        int count = (int) (intensity * 20);
        for (int i = 0; i < count; i++) {
            double px = bounds.minX + level.random.nextDouble() * (bounds.maxX - bounds.minX);
            double py = bounds.minY + level.random.nextDouble() * 0.5; // Bottom part mainly
            double pz = bounds.minZ + level.random.nextDouble() * (bounds.maxZ - bounds.minZ);
            
            // Randomly offset slightly downwards to simulate air trail
            level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, -0.2, 0);
            if (level.random.nextFloat() < intensity) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, 0, 0.1, 0);
            }
        }
    }
}
