package dev.ryanhcode.sable.neoforge;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.FlywheelCompatNeoForge;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Sable.MOD_ID, dist = Dist.CLIENT)
public final class SableNeoForgeClient {

    public SableNeoForgeClient(final ModContainer modContainer, final IEventBus modBus) {
        final IEventBus neoBus = NeoForge.EVENT_BUS;

        SableClient.init();

        modContainer.registerConfig(ModConfig.Type.CLIENT, SableClientConfig.SPEC);
        modBus.<ModConfigEvent.Loading>addListener(event -> SableClientConfig.onUpdate(false));
        modBus.<ModConfigEvent.Reloading>addListener(event -> SableClientConfig.onUpdate(true));
        neoBus.<ClientPlayerNetworkEvent.LoggingOut>addListener(event -> {
            if (event.getPlayer() != null) { // LoggingOut may fire when logging in
                FloatingBlockMaterialDataHandler.clearMaterials();
            }
        });
        modBus.<RegisterClientReloadListenersEvent>addListener(event -> event.registerReloadListener((arg, arg2, arg3, arg4, executor, executor2) -> SubLevelRenderDispatcher.get().reload(arg, arg2, arg3, arg4, executor, executor2)));

        if (FlywheelCompatNeoForge.FLYWHEEL_LOADED) {
            Sable.LOGGER.warn("NOTE: Sable is loaded with Flywheel. Sable contains extensive shader overrides and a full light-storage replacement. Expect this to cause compatibility issues. If issues arise, please report them to the Sable issue tracker ({}) instead of the Flywheel issue tracker.", Sable.ISSUE_TRACKER_URL);
        }
    }
}
