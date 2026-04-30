package dev.devce.rocketnautics.event;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.screens.AstralEngineeringTableScreen;
import dev.devce.rocketnautics.content.screens.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientModEvents {

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientModEvents::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                ModMenuTypes.ASTRAL_ENGINEERING_TABLE_MENU.get(),
                AstralEngineeringTableScreen::new
        );
    }
}