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
import dev.devce.rocketnautics.network.NetworkHandler;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import dev.devce.rocketnautics.content.blocks.nodes.LinkedSignalHandler;

/**
 * Main class for the Cosmonautics (RocketNautics) mod.
 * This class handles mod initialization, configuration registration, 
 * and various system setups for physics, commands, and registry.
 */
@Mod(RocketNautics.MODID)
public class RocketNautics {
    /** The unique identifier for this mod. */
    public static final String MODID = "rocketnautics";
    /** Global logger instance for this mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Constructor for the mod. Performs initial registration of configs, blocks, and handlers.
     * 
     * @param modEventBus The event bus for mod-specific events.
     * @param modContainer The container for this mod instance.
     */
    public RocketNautics(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        LOGGER.info("Initializing Cosmonautics!");
        
        // Register mod configurations
        modContainer.registerConfig(ModConfig.Type.SERVER, (net.neoforged.fml.config.IConfigSpec) RocketConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, (net.neoforged.fml.config.IConfigSpec) RocketConfig.CLIENT_SPEC);

        // Attempt to expand Sable altitude limits to allow for high-altitude flight
        try {
            dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MIN.set(-1000000.0);
            dev.ryanhcode.sable.SableConfig.SUB_LEVEL_REMOVE_MAX.set(1000000.0);
            LOGGER.info("Successfully expanded Sable altitude limits for Cosmonautics.");
        } catch (Exception e) {
            LOGGER.error("Failed to override SableConfig: {}", e.getMessage());
        }

        // Register registries
        RocketBlocks.register(modEventBus);
        dev.devce.rocketnautics.registry.RocketTabs.register(modEventBus);
        RocketBlockEntities.register(modEventBus);
        RocketParticles.register(modEventBus);
        RocketSounds.register(modEventBus);
        RocketSimulatedTab.init();

        // Register mod-bus event subscribers manually to avoid deprecated bus() parameter
        modEventBus.register(NetworkHandler.class);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            modEventBus.register(dev.devce.rocketnautics.client.ClientModEvents.class);
            modEventBus.register(RocketNauticsClient.class);
            NeoForge.EVENT_BUS.register(dev.devce.rocketnautics.client.RocketNauticsClientEvents.class);
        }

        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(this);
        
        // Initialize physics and game mechanics handlers
        GlobalSpacePhysicsHandler.init();
        dev.devce.rocketnautics.content.physics.AsteroidSpawner.init();
        dev.devce.rocketnautics.content.physics.SpaceTransitionHandler.init();
    }

    /**
     * Common setup logic that runs during the mod loading phase.
     */
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Cosmonautics Setup");
        // Ensure Sable altitude limits are set even if constructor override was too early
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

    /**
     * Registers console commands for the mod.
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GravityCommand.register(event.getDispatcher());
        ShipCopyPasteCommand.register(event.getDispatcher());
        JetpackCommand.register(event.getDispatcher());
        dev.devce.rocketnautics.content.commands.AsteroidCommand.register(event.getDispatcher());
    }
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        LinkedSignalHandler.tick(event.getLevel());
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
            LinkedSignalHandler.onWorldUnload(level);
        }
    }
}
