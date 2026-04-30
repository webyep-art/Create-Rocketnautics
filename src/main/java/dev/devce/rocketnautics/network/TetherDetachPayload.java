package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TetherDetachPayload() implements CustomPacketPayload {
    public static final Type<TetherDetachPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "tether_detach"));

    public static final StreamCodec<ByteBuf, TetherDetachPayload> CODEC =
            StreamCodec.unit(new TetherDetachPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
