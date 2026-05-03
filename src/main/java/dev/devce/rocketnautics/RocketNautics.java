package dev.devce.rocketnautics;

import dev.devce.rocketnautics.content.mobs.Starved;
import dev.devce.rocketnautics.content.screens.ModMenuTypes;
import dev.devce.rocketnautics.event.ClientModEvents;
import dev.devce.rocketnautics.event.RopeHandler;
import dev.devce.rocketnautics.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.devce.rocketnautics.content.commands.GravityCommand;
import net.neoforged.fml.common.Mod;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(RocketNautics.MODID)
public class RocketNautics {
    public static final String MODID = "rocketnautics";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RocketNautics(IEventBus modEventBus) {
        LOGGER.info("Initializing RocketNautics!");

        RocketBlocks.register(modEventBus);
        RocketItems.register(modEventBus);
        RocketEntities.register(modEventBus);
        ArmorMaterials.register(modEventBus);
        RocketBlockEntities.register(modEventBus);
        RocketParticles.register(modEventBus);
        RocketTabs.register(modEventBus);
        RocketSounds.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(this);
        GlobalSpacePhysicsHandler.init();
        RopeHandler.init(modEventBus);
        ClientModEvents.init(modEventBus);

    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("RocketNautics Setup");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GravityCommand.register(event.getDispatcher());
    }
}
