package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import dev.devce.rocketnautics.content.blocks.nodes.WirelessDataHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class DataTransmitterNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Data Transmitter"); }

    @Override
    public String getCategory() { return "Wireless"; }

    @Override
    public Component getDescription() {
        return Component.literal("Broadcasts data over a named channel.\n\n" +
            "Input 1: Data to send\n" +
            "Click node body to set Channel Name.");
    }

    @Override
    public int getInputCount() { return 1; }

    @Override
    public boolean isTrigger() { return true; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double val = context.evaluateInput(node.id, 0);
        String channel = node.commentText;
        WirelessDataHandler.setData(channel, val);
        return val;
    }

    @Override
    public int renderCustomUI(GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        String channel = node.commentText;
        if (channel.isEmpty()) channel = "NOT_SET";
        
        int cy = y + 10;
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "CH: " + channel, x + width / 2, cy, 0xFF00FF88);
        return 20;
    }

    @Override
    public String getIcon() { return "📡"; }
    @Override
    public int getHeaderColor() { return 0xFF00AAFF; }
}
