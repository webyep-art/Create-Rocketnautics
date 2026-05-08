package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;

import java.util.function.ToDoubleFunction;

public class SensorNodeHandler implements NodeHandler {
    private final String name;
    private final String description;
    private final ToDoubleFunction<NodeContext> sensor;
    private final int color;

    public SensorNodeHandler(String name, String description, ToDoubleFunction<NodeContext> sensor, int color) {
        this.name = name;
        this.description = description;
        this.sensor = sensor;
        this.color = color;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(name);
    }

    @Override
    public Component getDescription() {
        return Component.literal(description);
    }

    @Override
    public String getCategory() {
        return "Sensors";
    }

    @Override
    public int renderCustomUI(net.minecraft.client.gui.GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        int graphW = width - 10;
        int graphH = 20;
        int gx = x + 5;
        int gy = y + 5;

        // Background
        graphics.fill(gx, gy, gx + graphW, gy + graphH, 0x33000000);
        
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
            
            graphics.fill((int)x1, (int)y1, (int)x2 + 1, (int)y1 + 1, color | 0xFF000000);
            // Draw vertical line to connect points if needed, but fill is enough for a small graph
        }

        return graphH + 5;
    }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double val = sensor.applyAsDouble(context);
        
        // Update history every 2 ticks (approx 0.1s)
        if (context.getLevel().getGameTime() % 2 == 0) {
            node.history[node.historyIndex] = val;
            node.historyIndex = (node.historyIndex + 1) % node.history.length;
        }
        
        return val;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public String getIcon() {
        return name.substring(0, 1).toUpperCase();
    }

    @Override
    public int getHeaderColor() {
        return color;
    }
}
