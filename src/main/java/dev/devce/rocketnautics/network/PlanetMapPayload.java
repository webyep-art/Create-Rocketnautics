package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanetMapPayload(int powerSize, int centerX, int centerZ, byte[] mapDataPosXPosZ, byte[] mapDataPosXNegZ, byte[] mapDataNegXPosZ, byte[] mapDataNegXNegZ) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlanetMapPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "planet_map_data"));
    
    public static final StreamCodec<ByteBuf, PlanetMapPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeInt(payload.powerSize());
            buf.writeInt(payload.centerX());
            buf.writeInt(payload.centerZ());
            buf.writeBytes(payload.mapDataPosXPosZ());
            buf.writeBytes(payload.mapDataPosXNegZ());
            buf.writeBytes(payload.mapDataNegXPosZ());
            buf.writeBytes(payload.mapDataNegXNegZ());
        },
        buf -> {
            int powerSize = buf.readInt();
            int negXCorner = buf.readInt();
            int negZCorner = buf.readInt();
            byte[] posXPosZ = new byte[256 * 256];
            buf.readBytes(posXPosZ);
            byte[] posXNegZ = new byte[256 * 256];
            buf.readBytes(posXNegZ);
            byte[] negXPosZ = new byte[256 * 256];
            buf.readBytes(negXPosZ);
            byte[] negXNegZ = new byte[256 * 256];
            buf.readBytes(negXNegZ);
            return new PlanetMapPayload(powerSize, negXCorner, negZCorner, posXPosZ, posXNegZ, negXPosZ, negXNegZ);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
