package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DebugLogPayload(String message, int color) implements CustomPacketPayload {
    public static final Type<DebugLogPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "debug_log"));
    public static final StreamCodec<FriendlyByteBuf, DebugLogPayload> CODEC = StreamCodec.of(
        (buf, val) -> {
            buf.writeUtf(val.message);
            buf.writeInt(val.color);
        },
        buf -> new DebugLogPayload(buf.readUtf(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
