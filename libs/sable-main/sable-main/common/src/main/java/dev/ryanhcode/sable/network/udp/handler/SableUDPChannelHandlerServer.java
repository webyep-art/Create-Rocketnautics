package dev.ryanhcode.sable.network.udp.handler;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.udp.ServerConnectionListenerExtension;
import dev.ryanhcode.sable.network.udp.AddressedSableUDPPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnectionListener;

public class SableUDPChannelHandlerServer extends SimpleChannelInboundHandler<AddressedSableUDPPacket> {

    private final MinecraftServer server;
    private final ServerConnectionListener serverConnectionListener;

    public SableUDPChannelHandlerServer(final MinecraftServer server, final ServerConnectionListener serverConnectionListener) {
        super(AddressedSableUDPPacket.class);
        this.server = server;
        this.serverConnectionListener = serverConnectionListener;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Sable.LOGGER.error("Server UDP channel caught exception", cause);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        Sable.LOGGER.info("Server UDP channel active");
        ((ServerConnectionListenerExtension) this.serverConnectionListener).sable$setupUDPServer(ctx.channel());
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final AddressedSableUDPPacket msg) throws Exception {
        msg.packet().handleServer(this.server, msg.address());
    }
}
