package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.api.peripherals.IPeripheral;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;
import java.util.List;

public class VectorNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Vector Engine"); }

    @Override
    public String getCategory() { return "Peripheral"; }

    @Override
    public Component getDescription() {
        return Component.literal("Advanced control for vectoring engines.\n\n" +
            "In 1: Thrust (0-1)\n" +
            "In 2: Gimbal X (-1 to 1)\n" +
            "In 3: Gimbal Z (-1 to 1)");
    }

    @Override
    public int getInputCount() { return 4; }

    @Override
    public List<Component> getInputNames() {
        return List.of(Component.literal("THROTTLE"), Component.literal("GIMBAL X"), Component.literal("GIMBAL Z"), Component.literal("ENGINE ID"));
    }

    @Override
    public boolean isTrigger() { return true; }

    @Override
    public int renderCustomUI(net.minecraft.client.gui.GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        int size = 40;
        int cx = x + width / 2;
        int cy = y + size / 2 + 5;

        // Background box
        graphics.fill(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, 0x44000000);
        graphics.renderOutline(cx - size / 2, cy - size / 2, size, size, 0x33FFFFFF);

        // Crosshair lines
        graphics.fill(cx - size / 2 + 2, cy, cx + size / 2 - 2, cy + 1, 0x22FFFFFF);
        graphics.fill(cx, cy - size / 2 + 2, cx + 1, cy + size / 2 - 2, 0x22FFFFFF);

        // Current Gimbal Point
        double gx = node.lastGimbalX;
        double gz = node.lastGimbalZ;
        
        int px = cx + (int)(gx * (size / 2 - 4));
        int py = cy + (int)(gz * (size / 2 - 4));

        graphics.fill(px - 1, py - 1, px + 2, py + 2, 0xFF00FF88);
        
        return size + 10;
    }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double thrust = context.evaluateInput(node.id, 0);
        double gx = context.evaluateInput(node.id, 1);
        double gz = context.evaluateInput(node.id, 2);
        double idVal = context.evaluateInput(node.id, 3);
        int engineIdx = (int) idVal;
        
        node.lastGimbalX = gx;
        node.lastGimbalZ = gz;
        
        var peripherals = context.getPeripherals();
        if (engineIdx >= 0 && engineIdx < peripherals.size()) {
            IPeripheral p = peripherals.get(engineIdx);
            if (p.getPeripheralType().equals("vector_engine")) {
                p.writeValue("thrust", thrust);
                p.writeValues("gimbal", gx * 180.0, gz * 180.0);
            }
        }
        
        return thrust;
    }

    @Override
    public boolean isAvailable(NodeContext context) {
        return context.getPeripherals().stream().anyMatch(p -> p.getPeripheralType().equals("vector_engine"));
    }

    @Override
    public String getIcon() { return "V"; }

    @Override
    public int getHeaderColor() { return 0xFF00AAFF; }
}
