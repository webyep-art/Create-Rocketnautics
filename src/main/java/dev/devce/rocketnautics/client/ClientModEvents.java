package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.RocketNauticsClient;
import dev.devce.rocketnautics.client.render.JetpackLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import net.minecraft.client.resources.PlayerSkin;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(RocketNauticsClient.JETPACK_TOGGLE);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        
        for (PlayerSkin.Model model : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(model);
            if (renderer != null) {
                renderer.addLayer(new JetpackLayer<>(renderer));
            }
        }
    }
}
