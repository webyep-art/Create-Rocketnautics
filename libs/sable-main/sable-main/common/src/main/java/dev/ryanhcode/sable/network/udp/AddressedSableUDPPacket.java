package dev.ryanhcode.sable.network.udp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A {@link SableUDPPacket} that has been addressed to a specific {@link SocketAddress}
 */
public record AddressedSableUDPPacket(SableUDPPacket packet, InetSocketAddress address) {
}
