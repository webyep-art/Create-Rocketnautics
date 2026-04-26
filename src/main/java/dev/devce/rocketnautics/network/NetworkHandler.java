package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
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
            (payload, context) -> context.enqueueWork(() -> handleMapRequest(context.player()))
        );

        registrar.playToClient(
            PlanetMapPayload.TYPE,
            PlanetMapPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleMapData(payload.mapData()))
        );
    }

    private static void handleMapRequest(net.minecraft.world.entity.player.Player rawPlayer) {
        if (!(rawPlayer instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        
        // Run generation async to avoid server lag
        CompletableFuture.runAsync(() -> {
            try {
                BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
                Climate.Sampler sampler = level.getChunkSource().randomState().sampler();

                byte[] mapData = new byte[65536]; // 256x256
                int step = 128; // 128 blocks per pixel. Total area = 32768x32768 blocks
                int startX = (int) player.getX() - (256 * step) / 2;
                int startZ = (int) player.getZ() - (256 * step) / 2;

                for (int x = 0; x < 256; x++) {
                    for (int z = 0; z < 256; z++) {
                        int worldX = startX + x * step;
                        int worldZ = startZ + z * step;
                        
                        Holder<Biome> biome = source.getNoiseBiome(worldX >> 2, 64 >> 2, worldZ >> 2, sampler);
                        byte colorIdx = 4; // Default plains/grass
                        
                        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)) colorIdx = 0;
                        else if (biome.is(BiomeTags.IS_RIVER)) colorIdx = 1;
                        else if (biome.is(BiomeTags.IS_BEACH)) colorIdx = 2;
                        else if (biome.is(BiomeTags.HAS_DESERT_PYRAMID)) colorIdx = 3;
                        else if (biome.is(BiomeTags.IS_FOREST)) colorIdx = 5;
                        else if (biome.is(BiomeTags.IS_JUNGLE)) colorIdx = 6;
                        else if (biome.is(BiomeTags.IS_TAIGA)) colorIdx = 7;
                        else if (biome.is(BiomeTags.HAS_VILLAGE_SNOWY)) colorIdx = 8;
                        else if (biome.is(BiomeTags.IS_BADLANDS)) colorIdx = 9;
                        else if (biome.is(BiomeTags.IS_MOUNTAIN)) colorIdx = 10;
                        
                        mapData[x + z * 256] = colorIdx;
                    }
                }
                
                // Send back on main thread
                level.getServer().execute(() -> {
                    PacketDistributor.sendToPlayer(player, new PlanetMapPayload(mapData));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handleMapData(byte[] data) {
        SkyHandler.updatePlanetTexture(data);
    }
}
