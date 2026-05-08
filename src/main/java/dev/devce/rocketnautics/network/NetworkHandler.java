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
import dev.devce.rocketnautics.network.SputnikNodeSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.CompletableFuture;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(RocketNautics.MODID).versioned("1.0");
        
        registrar.playToServer(
            JetpackTogglePayload.TYPE,
            JetpackTogglePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> dev.devce.rocketnautics.content.physics.JetpackHandler.toggle((ServerPlayer) context.player()))
        );

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

        registrar.playToClient(
            JetpackPayload.TYPE,
            JetpackPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleJetpackState(payload.entityId(), payload.active()))
        );

        registrar.playToServer(
            SputnikNodeSyncPayload.TYPE,
            SputnikNodeSyncPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleSputnikSync(context.player(), payload.pos(), payload.graphData()))
        );
    }

    private static void handleSputnikSync(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, net.minecraft.nbt.CompoundTag graphData) {
        net.minecraft.world.level.Level foundLevel = null;
        if (player.level().getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity) {
            foundLevel = player.level();
        } else {
            // Check all levels if not in current player level (e.g. ship in space)
            for (net.minecraft.server.level.ServerLevel serverLevel : player.getServer().getAllLevels()) {
                if (serverLevel.getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity) {
                    foundLevel = serverLevel;
                    break;
                }
            }
        }

        if (foundLevel != null && foundLevel.getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity sputnik) {
            sputnik.graph.nodes.clear();
            sputnik.graph.connections.clear();
            dev.devce.rocketnautics.content.blocks.nodes.NodeGraph loaded = new dev.devce.rocketnautics.content.blocks.nodes.NodeGraph(graphData, foundLevel.registryAccess());
            sputnik.graph.nodes.addAll(loaded.nodes);
            sputnik.graph.connections.addAll(loaded.connections);
            sputnik.graph.clearCache();
            
            // Force immediate refresh to ensure server sees engines before next tick
            sputnik.refreshEngines();
            sputnik.setChanged();
            
            if (dev.devce.rocketnautics.RocketConfig.SERVER.enableEngineDebugLogging.get()) {
                dev.devce.rocketnautics.RocketNautics.LOGGER.info("Sputnik at {} (level {}) SYNCED. Nodes: {}, Connections: {}, Engines Found: {}", 
                    pos, foundLevel.dimension().location(), sputnik.graph.nodes.size(), sputnik.graph.connections.size(), sputnik.getEngineCount());
                for (var node : sputnik.graph.nodes) {
                    dev.devce.rocketnautics.RocketNautics.LOGGER.info("  Node: {} type={} idx={}", node.id, node.typeId, node.engineIndex);
                }
            }
        } else {
            dev.devce.rocketnautics.RocketNautics.LOGGER.warn("Failed to find Sputnik at {} for sync from player {}", pos, player.getName().getString());
        }
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handleJetpackState(int entityId, boolean active) {
        dev.devce.rocketnautics.content.physics.JetpackHandler.setEntityActive(entityId, active);
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
        ServerLevel level = player.serverLevel();

        
        CompletableFuture.runAsync(() -> {
            SkyDataHandler handler = SkyDataHandler.getHandlerForLevel(level);
            PlanetMapPayload payload = handler.getRenderDataAtScaleAndPosition(powerSize, player.getBlockX(), player.getBlockZ());
            
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
