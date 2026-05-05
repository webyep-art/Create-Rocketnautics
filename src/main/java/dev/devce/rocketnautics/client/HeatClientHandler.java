package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketNautics;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.joml.Vector3d;
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
    private static final Map<Vec3, Float> HEAT_MAP = new HashMap<>();

    public static void updateHeat(double x, double y, double z, float intensity) {
        HEAT_MAP.put(new Vec3(x, y, z), intensity);
    }

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) return;
        
        ClientLevel level = (ClientLevel) event.getLevel();
        if (level == null) {
            HEAT_MAP.clear();
            return;
        }

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;

        HEAT_MAP.entrySet().removeIf(entry -> {
            Vec3 targetPos = entry.getKey();
            float intensity = entry.getValue();

            
            SubLevel matchingSubLevel = null;
            double minDist = 25.0; 
            
            for (SubLevel sl : container.getAllSubLevels()) {
                Vector3d pos = sl.logicalPose().position();
                double dist = targetPos.distanceToSqr(pos.x, pos.y, pos.z);
                if (dist < minDist) {
                    minDist = dist;
                    matchingSubLevel = sl;
                }
            }

            if (matchingSubLevel != null && intensity > 0.05f) {
                spawnHeatParticles(level, matchingSubLevel, intensity);
                entry.setValue(intensity * 0.95f);
                return false;
            }
            return true;
        });
    }

    private static void spawnHeatParticles(ClientLevel level, SubLevel subLevel, float intensity) {
        Vector3d pos = subLevel.logicalPose().position();
        if (pos == null) return;

        
        int count = (int) (intensity * 15); 
        for (int i = 0; i < count; i++) {
            double px = pos.x + (level.random.nextDouble() - 0.5) * 4.5;
            double py = pos.y + (level.random.nextDouble() - 0.5) * 2.0; 
            double pz = pos.z + (level.random.nextDouble() - 0.5) * 4.5;
            
            
            double trailSpeed = 0.4 * intensity;
            level.addParticle(dev.devce.rocketnautics.registry.RocketParticles.BLUE_FLAME.get(), px, py, pz, 
                (level.random.nextDouble() - 0.5) * 0.1, trailSpeed, (level.random.nextDouble() - 0.5) * 0.1);
            
            if (level.random.nextFloat() < intensity * 0.7) {
                level.addParticle(dev.devce.rocketnautics.registry.RocketParticles.JET_SMOKE.get(), px, py, pz, 
                    (level.random.nextDouble() - 0.5) * 0.3, trailSpeed * 0.5, (level.random.nextDouble() - 0.5) * 0.3);
            }
        }
    }
}
