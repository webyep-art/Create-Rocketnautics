package dev.devce.rocketnautics;

import dev.devce.rocketnautics.registry.RocketBlocks;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketParticles;
import dev.devce.rocketnautics.registry.RocketSounds;
import dev.devce.rocketnautics.registry.RocketTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.devce.rocketnautics.content.commands.GravityCommand;
import dev.devce.rocketnautics.content.commands.ShipCopyPasteCommand;
import net.neoforged.fml.common.Mod;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(RocketNautics.MODID)
public class RocketNautics {
    public static final String MODID = "rocketnautics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RocketNautics(IEventBus modEventBus) {
        LOGGER.info("Initializing RocketNautics!");

        RocketBlocks.register(modEventBus);
        RocketBlockEntities.register(modEventBus);
        RocketParticles.register(modEventBus);
        RocketTabs.register(modEventBus);
        RocketSounds.register(modEventBus);

        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(this);
        GlobalSpacePhysicsHandler.init();
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("RocketNautics Setup");
        
        // Расширяем лимиты высоты Sable для наших орбит
        try {
            dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MIN.set(-1000000.0);
            dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MAX.set(1000000.0);
            LOGGER.info("Successfully expanded Sable altitude limits for RocketNautics.");
        } catch (Exception e) {
            LOGGER.error("Failed to override SableConfig: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GravityCommand.register(event.getDispatcher());
        ShipCopyPasteCommand.register(event.getDispatcher());
    }
}
