package dev.devce.rocketnautics.event;

import dev.devce.rocketnautics.content.mobs.StarvedRenderer;
import dev.devce.rocketnautics.content.screens.AstralEngineeringTableScreen;
import dev.devce.rocketnautics.content.screens.ModMenuTypes;
import dev.devce.rocketnautics.registry.RocketEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientModEvents {

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientModEvents::registerScreens);
        modBus.addListener(ClientModEvents::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                ModMenuTypes.ASTRAL_ENGINEERING_TABLE_MENU.get(),
                AstralEngineeringTableScreen::new
        );
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RocketEntities.STARVED.get(), StarvedRenderer::new);
    }
}