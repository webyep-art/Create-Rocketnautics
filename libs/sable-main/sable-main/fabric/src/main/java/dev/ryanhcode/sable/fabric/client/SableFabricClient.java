package dev.ryanhcode.sable.fabric.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.config.ModConfig;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SableFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SableClient.init();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FloatingBlockMaterialDataHandler.clearMaterials());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return Sable.sablePath("sub_level_renderer");
            }

            @Override
            public @NotNull CompletableFuture<Void> reload(final PreparationBarrier preparationBarrier, final ResourceManager resourceManager, final ProfilerFiller profilerFiller, final ProfilerFiller profilerFiller2, final Executor executor, final Executor executor2) {
                return SubLevelRenderer.getDispatcher().reload(preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2);
            }
        });

        NeoForgeModConfigEvents.loading(Sable.MOD_ID).register(config -> {
            if (config.getSpec().equals(SableClientConfig.SPEC))
                SableClientConfig.onUpdate(false);
        });

        NeoForgeModConfigEvents.reloading(Sable.MOD_ID).register(config -> {
            if (config.getSpec().equals(SableClientConfig.SPEC))
                SableClientConfig.onUpdate(true);
        });

        NeoForgeConfigRegistry.INSTANCE.register(Sable.MOD_ID, ModConfig.Type.CLIENT, SableClientConfig.SPEC);
    }
}
