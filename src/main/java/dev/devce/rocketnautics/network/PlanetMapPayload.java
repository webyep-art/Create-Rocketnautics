package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanetMapPayload(byte[] mapData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlanetMapPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "planet_map_data"));
    
    public static final StreamCodec<ByteBuf, PlanetMapPayload> CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeBytes(payload.mapData()),
        buf -> {
            byte[] data = new byte[65536]; // 256x256
            buf.readBytes(data);
            return new PlanetMapPayload(data);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
