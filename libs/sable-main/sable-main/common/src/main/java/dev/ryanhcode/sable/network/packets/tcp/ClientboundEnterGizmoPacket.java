package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundEnterGizmoPacket() implements SableTCPPacket {

    public static final Type<ClientboundEnterGizmoPacket> TYPE = new Type<>(Sable.sablePath("enter_gizmo_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundEnterGizmoPacket> CODEC = StreamCodec.of((buf, value) -> value.write(buf), ClientboundEnterGizmoPacket::read);

    private static ClientboundEnterGizmoPacket read(final FriendlyByteBuf buf) {
        return new ClientboundEnterGizmoPacket();
    }

    private void write(final FriendlyByteBuf buf) {

    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(final PacketContext context) {
        SableClient.GIZMO_HANDLER.start();
    }
}