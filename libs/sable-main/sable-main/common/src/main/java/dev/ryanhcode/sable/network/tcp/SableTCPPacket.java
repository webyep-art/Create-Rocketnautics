package dev.ryanhcode.sable.network.tcp;

import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface SableTCPPacket extends CustomPacketPayload {

    void handle(PacketContext context);
}
