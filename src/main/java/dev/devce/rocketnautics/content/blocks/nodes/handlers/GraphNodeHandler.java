package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;

public class GraphNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Oscilloscope"); }

    @Override
    public String getCategory() { return "Display"; }

    @Override
    public Component getDescription() {
        return Component.literal("Displays a high-resolution graph of the input signal over time.\n\n" +
            "Useful for tuning PIDs and observing oscillations.");
    }

    @Override
    public int getInputCount() { return 1; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double val = context.evaluateInput(node.id, 0);
        
        // Update history every tick for high resolution
        node.history[node.historyIndex] = val;
        node.historyIndex = (node.historyIndex + 1) % node.history.length;
        
        return val;
    }

    @Override
    public int renderCustomUI(net.minecraft.client.gui.GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        int graphW = width - 10;
        int graphH = 40;
        int gx = x + 5;
        int gy = y + 5;

        // Background
        graphics.fill(gx, gy, gx + graphW, gy + graphH, 0x66000000);
        graphics.renderOutline(gx, gy, graphW, graphH, 0x22FFFFFF);
        
        // Grid lines
        graphics.fill(gx, gy + graphH / 2, gx + graphW, gy + graphH / 2 + 1, 0x11FFFFFF);
        
        // Draw sparkline
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        for (double v : node.history) {
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        
        double range = max - min;
        if (range < 0.01) range = 1.0;

        for (int i = 0; i < node.history.length - 1; i++) {
            int idx1 = (node.historyIndex + i) % node.history.length;
            int idx2 = (node.historyIndex + i + 1) % node.history.length;
            
            float x1 = gx + (i * (float)graphW / node.history.length);
            float x2 = gx + ((i + 1) * (float)graphW / node.history.length);
            
            float y1 = gy + graphH - (float)((node.history[idx1] - min) / range * graphH);
            float y2 = gy + graphH - (float)((node.history[idx2] - min) / range * graphH);
            
            graphics.fill((int)x1, (int)y1, (int)x2 + 1, (int)y1 + 1, 0xFF00FF88);
        }

        // Value text
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawString(font, String.format("%.2f", currentVal), gx + 2, gy + 2, 0xFF00FF88);

        return graphH + 10;
    }

    @Override
    public String getIcon() { return "G"; }

    @Override
    public int getHeaderColor() { return 0xFF00FF88; }
}
