package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.LinkedSignalHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class LinkedOutputNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Linked Transmitter"); }

    @Override
    public String getCategory() { return "Wireless"; }

    @Override
    public Component getDescription() {
        return Component.literal("Transmits redstone signals to the Create wireless network.\n\n" +
            "Input (0-15) sets the signal strength.\n" +
            "Click Red/Blue slots to set frequency items.");
    }

    @Override
    public int getInputCount() { return 1; }
    @Override
    public int getOutputCount() { return 0; }

    @Override
    public boolean isTrigger() { return true; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double val = context.evaluateInput(node.id, 0);
        LinkedSignalHandler.setSignal(context.getLevel(), node.freqStack1, node.freqStack2, context.getBlockPos(), val);
        return val;
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
    public String getIcon() { return "T"; }
    @Override
    public int getHeaderColor() { return 0xFFFF5555; }
}
