package dev.ryanhcode.sable.network.packets.udp;

import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacketType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public record SableUDPEchoPacket(String text) implements SableUDPPacket {
    public static final StreamCodec<RegistryFriendlyByteBuf, SableUDPEchoPacket> CODEC = StreamCodec.of((buf, value) -> buf.writeUtf(value.text), buf -> new SableUDPEchoPacket(buf.readUtf()));

    @Override
    public SableUDPPacketType getType() {
        return SableUDPPacketType.PING;
    }

    @Override
    public void handleClient(final Level level) {
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Received UDP Test Ping: " + this.text));
    }
}
