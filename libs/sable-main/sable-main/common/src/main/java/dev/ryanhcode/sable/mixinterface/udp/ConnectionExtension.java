package dev.ryanhcode.sable.mixinterface.udp;

import io.netty.channel.Channel;

public interface ConnectionExtension {

    void sable$setUDPChannel(final Channel channel);

    Channel sable$getUDPChannel();

}
