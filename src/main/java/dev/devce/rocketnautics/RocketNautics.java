package dev.devce.rocketnautics;

import dev.devce.rocketnautics.registry.RocketBlocks;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketParticles;
import dev.devce.rocketnautics.registry.RocketSounds;
import dev.devce.rocketnautics.registry.RocketSimulatedTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.devce.rocketnautics.content.commands.GravityCommand;
import dev.devce.rocketnautics.content.commands.JetpackCommand;
import dev.devce.rocketnautics.content.commands.ShipCopyPasteCommand;
import net.neoforged.fml.common.Mod;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.fml.config.ModConfig;

@Mod(RocketNautics.MODID)
public class RocketNautics {
    public static final String MODID = "rocketnautics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RocketNautics(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        LOGGER.info("Initializing Cosmonautics!");
        
        modContainer.registerConfig(ModConfig.Type.SERVER, (net.neoforged.fml.config.IConfigSpec) RocketConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, (net.neoforged.fml.config.IConfigSpec) RocketConfig.CLIENT_SPEC);

        try {
            dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MIN.set(-1000000.0);
            dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MAX.set(1000000.0);
            LOGGER.info("Successfully expanded Sable altitude limits for Cosmonautics.");
        } catch (Exception e) {
            LOGGER.error("Failed to override SableConfig: {}", e.getMessage());
        }

        RocketBlocks.register(modEventBus);
        dev.devce.rocketnautics.registry.RocketTabs.register(modEventBus);
        RocketBlockEntities.register(modEventBus);
        RocketParticles.register(modEventBus);
        RocketSounds.register(modEventBus);
        RocketSimulatedTab.init();

        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(this);
        GlobalSpacePhysicsHandler.init();
        dev.devce.rocketnautics.content.physics.AsteroidSpawner.init();
        dev.devce.rocketnautics.content.physics.SpaceTransitionHandler.init();
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Cosmonautics Setup");
        event.enqueueWork(() -> {
            try {
                dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MIN.set(-1000000.0);
                dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MAX.set(1000000.0);
                LOGGER.info("Successfully expanded Sable altitude limits via enqueueWork.");
            } catch (Exception e) {
                LOGGER.error("Failed to set SableConfig in enqueueWork: {}", e.getMessage());
            }
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GravityCommand.register(event.getDispatcher());
        ShipCopyPasteCommand.register(event.getDispatcher());
        JetpackCommand.register(event.getDispatcher());
        dev.devce.rocketnautics.content.commands.AsteroidCommand.register(event.getDispatcher());
    }
}
