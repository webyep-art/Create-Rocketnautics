package dev.devce.rocketnautics.content.blocks.nodes;

import java.util.HashMap;
import java.util.Map;

/**
 * Global bus for wireless data transmission between flight computers.
 * Allows sending arbitrary double values over named channels.
 */
public class WirelessDataHandler {
    private static final Map<String, Double> channels = new HashMap<>();

    public static void setData(String channel, double value) {
        if (channel == null || channel.isEmpty()) return;
        channels.put(channel, value);
    }

    public static double getData(String channel) {
        if (channel == null || channel.isEmpty()) return 0.0;
        return channels.getOrDefault(channel, 0.0);
    }
}
