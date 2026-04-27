package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ReentryHeatPayload(UUID subLevelId, float intensity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReentryHeatPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "reentry_heat"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ReentryHeatPayload> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, ReentryHeatPayload::subLevelId,
        ByteBufCodecs.FLOAT, ReentryHeatPayload::intensity,
        ReentryHeatPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Helper class for UUID streaming since UUIDUtil.STREAM_CODEC might not be directly available in all versions
    private static class UUIDUtil {
        public static final StreamCodec<RegistryFriendlyByteBuf, UUID> STREAM_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
        );
    }
}
