package dev.ryanhcode.sable.network.udp;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.flow.FlowControlHandler;
import net.minecraft.network.*;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

public interface SableUDPPacket {

    static void configureSerialization(final ChannelPipeline pipeline, final PacketFlow flow, final boolean memoryOnly, @Nullable final BandwidthDebugMonitor debugMonitor) {
        pipeline.addLast("splitter", createFrameDecoder(debugMonitor, memoryOnly))
                .addLast(new FlowControlHandler())
                .addLast("decoder", new SableUDPPacketDecoder())
                .addLast("prepender", createFrameEncoder(memoryOnly))
                .addLast("encoder", new SableUDPPacketEncoder());

    }

    private static ChannelOutboundHandler createFrameEncoder(final boolean memoryOnly) {
        return memoryOnly ? new NoOpFrameEncoder() : new Varint21LengthFieldPrepender();
    }

    private static ChannelInboundHandler createFrameDecoder(@Nullable final BandwidthDebugMonitor debugMonitor, final boolean memoryOnly) {
        if (!memoryOnly) {
            return new Varint21FrameDecoder(debugMonitor);
        } else {
            return debugMonitor != null ? new MonitorFrameDecoder(debugMonitor) : new NoOpFrameDecoder();
        }
    }

    static void configureInMemoryPipeline(final ChannelPipeline channelPipeline, final PacketFlow arg) {
        configureSerialization(channelPipeline, arg, true, null);
    }

    SableUDPPacketType getType();

    default void handleClient(final Level level) {

    }

    default void handleServer(final MinecraftServer server, final InetSocketAddress sender) {

    }
}
