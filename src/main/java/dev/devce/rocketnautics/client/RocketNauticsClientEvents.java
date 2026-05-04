package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.RocketNauticsClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import dev.devce.rocketnautics.content.physics.JetpackHandler;
import net.minecraft.util.Mth;
import dev.devce.rocketnautics.client.CameraShakeHandler;
import dev.devce.rocketnautics.RocketConfig;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RocketNauticsClientEvents {
    private static float currentRoll = 0;
    private static float prevRoll = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (RocketNauticsClient.seamlessTransitionTicks > 0) {
            
            if (mc.options.renderDistance().get() > 2) {
                mc.options.renderDistance().set(2);
                RocketNauticsClient.lastAppliedRenderDistance = 2;
            }
            return;
        }

        
        int currentDist = mc.options.renderDistance().get();
        
        
        if (RocketNauticsClient.originalRenderDistance == -1) {
            double y = mc.player.getY();
            String dim = mc.level.dimension().location().getPath();
            boolean isAtForcedAltitude = (dim.equals("overworld") && y > 15000) || (dim.equals("space") && y < 800);
            
            
            
            if (currentDist > 2 || !isAtForcedAltitude) {
                RocketNauticsClient.originalRenderDistance = currentDist;
                RocketNauticsClient.lastAppliedRenderDistance = currentDist;
            }
        } else {
            
            if (RocketNauticsClient.lastAppliedRenderDistance != -1 && currentDist != RocketNauticsClient.lastAppliedRenderDistance) {
                RocketNauticsClient.originalRenderDistance = currentDist;
                RocketNauticsClient.lastAppliedRenderDistance = currentDist;
            }
        }

        if (RocketNauticsClient.originalRenderDistance != -1) {
            int targetDist = RocketNauticsClient.originalRenderDistance;

            
            if (RocketConfig.CLIENT.enableDynamicRenderDistance.get()) {
                double y = mc.player.getY();
                String dim = mc.level.dimension().location().getPath();
                
                if (dim.equals("overworld")) {
                    if (y > 19500) targetDist = 2;
                    else if (y > 19000) targetDist = 4;
                    else if (y > 18000) targetDist = 8;
                    else if (y > 15000) targetDist = 12;
                } else if (dim.equals("space")) {
                    if (y < 200) targetDist = 2;
                    else if (y < 400) targetDist = 4;
                    else if (y < 600) targetDist = 8;
                    else if (y < 800) targetDist = 12;
                }
            }

            
            if (targetDist != currentDist && mc.level.getGameTime() % 10 == 0) {
                int nextDist = currentDist < targetDist ? Math.min(currentDist + 2, targetDist) : Math.max(currentDist - 2, targetDist);
                mc.options.renderDistance().set(nextDist);
                RocketNauticsClient.lastAppliedRenderDistance = nextDist;
            }
        }

        
        while (RocketNauticsClient.JETPACK_TOGGLE.consumeClick()) {
            
            if (mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() instanceof dev.devce.rocketnautics.content.items.JetpackItem) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.devce.rocketnautics.network.JetpackTogglePayload());
            }
        }

        
        if (JetpackHandler.isActive(mc.player)) {
            if (mc.player.level().getGameTime() % 2 == 0) {
                
            }
        }

        
        CameraShakeHandler.tick();
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float[] angles = new float[]{event.getPitch(), event.getYaw(), event.getRoll()};
        CameraShakeHandler.applyShake((float)event.getPartialTick(), angles);
        event.setPitch(angles[0]);
        event.setYaw(angles[1]);
        event.setRoll(angles[2]);
    }
}
