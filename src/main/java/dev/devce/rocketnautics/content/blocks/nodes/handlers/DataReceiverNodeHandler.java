package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import dev.devce.rocketnautics.content.blocks.nodes.WirelessDataHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class DataReceiverNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Data Receiver"); }

    @Override
    public String getCategory() { return "Wireless"; }

    @Override
    public Component getDescription() {
        return Component.literal("Receives data from a named channel.\n\n" +
            "Output 1: Received data\n" +
            "Click node body to set Channel Name.");
    }

    @Override
    public int getOutputCount() { return 1; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        String channel = node.commentText;
        return WirelessDataHandler.getData(channel);
    }

    @Override
    public int renderCustomUI(GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        String channel = node.commentText;
        if (channel.isEmpty()) channel = "NOT_SET";
        
        int cy = y + 10;
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "CH: " + channel, x + width / 2, cy, 0xFFFFCC00);
        return 20;
    }

    @Override
    public String getIcon() { return "📡"; }
    @Override
    public int getHeaderColor() { return 0xFFFFAA00; }
}
