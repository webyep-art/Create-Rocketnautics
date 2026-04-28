package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.RocketNauticsClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RocketNauticsClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 1. Обработка таймера бесшовности (только блокировка экранов)
        if (RocketNauticsClient.seamlessTransitionTicks > 0) {
            RocketNauticsClient.seamlessTransitionTicks--;
            // Мы больше не форсируем восстановление здесь, 
            // чтобы оно не конфликтовало с логикой высоты ниже.
            return;
        }

        // 2. Динамическая дистанция прорисовки (дискретные шаги)
        double y = mc.player.getY();
        String dim = mc.level.dimension().location().getPath();
        int currentDist = mc.options.renderDistance().get();
        
        if (RocketNauticsClient.originalRenderDistance == -1) {
            RocketNauticsClient.originalRenderDistance = currentDist;
        }

        int targetDist = RocketNauticsClient.originalRenderDistance;

        if (dim.equals("overworld")) {
            if (y > 19500) targetDist = 2;
            else if (y > 19000) targetDist = 4;
            else if (y > 18000) targetDist = 8;
            else if (y > 15000) targetDist = 12;
        } else if (dim.equals("space")) {
            // В космосе чанков мало, можно восстанавливать быстрее
            if (y < 200) targetDist = 2;
            else if (y < 400) targetDist = 4;
            else if (y < 600) targetDist = 8;
            else if (y < 800) targetDist = 12;
        }

        // Обновляем только каждые 10 тиков (0.5 сек), чтобы не перегружать движок
        if (mc.level.getGameTime() % 10 == 0 && targetDist != currentDist) {
            int nextDist = currentDist < targetDist ? Math.min(currentDist + 2, targetDist) : Math.max(currentDist - 2, targetDist);
            mc.options.renderDistance().set(nextDist);
        }
    }
}
