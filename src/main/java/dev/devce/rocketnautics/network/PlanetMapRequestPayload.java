package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanetMapRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlanetMapRequestPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "planet_map_request"));
    
    public static final StreamCodec<ByteBuf, PlanetMapRequestPayload> CODEC = StreamCodec.unit(new PlanetMapRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
