package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SeamlessTransitionPayload(boolean active) implements CustomPacketPayload {
    public static final Type<SeamlessTransitionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "seamless_transition"));
    public static final StreamCodec<FriendlyByteBuf, SeamlessTransitionPayload> CODEC = StreamCodec.of(
        (buf, val) -> buf.writeBoolean(val.active),
        buf -> new SeamlessTransitionPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
