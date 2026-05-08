package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.LinkedSignalHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.List;

public class LinkedInputNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Linked Receiver"); }

    @Override
    public String getCategory() { return "Wireless"; }

    @Override
    public Component getDescription() {
        return Component.literal("Receives wireless redstone signals from the Create network.\n\n" +
            "Click Red/Blue slots to set frequency items.");
    }

    @Override
    public int getInputCount() { return 0; }
    @Override
    public int getOutputCount() { return 1; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        return LinkedSignalHandler.getSignal(context.getLevel(), node.freqStack1, node.freqStack2, context.getBlockPos());
    }

    @Override
    public int renderCustomUI(GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        int slotSize = 18;
        int padding = 5;
        int startX = x + width / 2 - slotSize - padding / 2;
        int startY = y + 5;

        // Red Slot
        graphics.fill(startX, startY, startX + slotSize, startY + slotSize, 0x44FF0000);
        graphics.renderOutline(startX, startY, slotSize, slotSize, 0xFF880000);
        if (!node.freqStack1.isEmpty()) {
            graphics.renderItem(node.freqStack1, startX + 1, startY + 1);
        }

        // Blue Slot
        int startX2 = startX + slotSize + padding;
        graphics.fill(startX2, startY, startX2 + slotSize, startY + slotSize, 0x440000FF);
        graphics.renderOutline(startX2, startY, slotSize, slotSize, 0xFF000088);
        if (!node.freqStack2.isEmpty()) {
            graphics.renderItem(node.freqStack2, startX2 + 1, startY + 1);
        }

        return 28;
    }

    @Override
    public String getIcon() { return "R"; }
    @Override
    public int getHeaderColor() { return 0xFF55FF55; }
}
