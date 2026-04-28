package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.SkyDataHandler;
import dev.devce.rocketnautics.client.SkyHandler;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(RocketNautics.MODID).versioned("1.0");
        
        registrar.playToServer(
            PlanetMapRequestPayload.TYPE,
            PlanetMapRequestPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleMapRequest(context.player(), payload.powerSize()))
        );

        registrar.playToClient(
            PlanetMapPayload.TYPE,
            PlanetMapPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleMapData(payload.powerSize(), payload.centerX(), payload.centerZ(), payload.mapDataPosXPosZ(), payload.mapDataPosXNegZ(), payload.mapDataNegXPosZ(), payload.mapDataNegXNegZ()))
        );

        registrar.playToClient(
            ReentryHeatPayload.TYPE,
            ReentryHeatPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleHeatData(payload.x(), payload.y(), payload.z(), payload.intensity()))
        );

        registrar.playToClient(
            SeamlessTransitionPayload.TYPE,
            SeamlessTransitionPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleSeamlessTransition(payload.active()))
        );

        registrar.playToClient(
            DebugLogPayload.TYPE,
            DebugLogPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> dev.devce.rocketnautics.RocketNauticsClient.addLog(payload.message(), payload.color()))
        );
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handleSeamlessTransition(boolean active) {
        if (active) {
            dev.devce.rocketnautics.RocketNauticsClient.startSeamlessTransition();
        } else {
            dev.devce.rocketnautics.RocketNauticsClient.endSeamlessTransition();
        }
    }

    private static void handleMapRequest(net.minecraft.world.entity.player.Player rawPlayer, int powerSize) {
        if (!(rawPlayer instanceof ServerPlayer player)) return;
        ServerLevel level = player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (level == null) return;
        
        // Run generation async to avoid server lag
        CompletableFuture.runAsync(() -> {
            SkyDataHandler handler = SkyDataHandler.getHandlerForLevel(level);
            PlanetMapPayload payload = handler.getRenderDataAtScaleAndPosition(powerSize, player.getBlockX(), player.getBlockZ());
            // Send back on main thread
            level.getServer().execute(() -> {
                PacketDistributor.sendToPlayer(player, payload);
            });
        });
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handleMapData(int powerSize, int centerX, int centerZ, byte[] mapDataPosXPosZ, byte[] mapDataPosXNegZ, byte[] mapDataNegXPosZ, byte[] mapDataNegXNegZ) {
        SkyHandler.updatePlanetTexture(powerSize, centerX, centerZ, mapDataPosXPosZ, mapDataPosXNegZ, mapDataNegXPosZ, mapDataNegXNegZ);
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handleHeatData(double x, double y, double z, float intensity) {
        dev.devce.rocketnautics.client.HeatClientHandler.updateHeat(x, y, z, intensity);
    }
}
