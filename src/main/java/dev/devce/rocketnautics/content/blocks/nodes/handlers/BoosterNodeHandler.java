package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.api.peripherals.IPeripheral;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;
import java.util.List;

public class BoosterNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Rocket Booster"); }

    @Override
    public String getCategory() { return "Peripheral"; }

    @Override
    public Component getDescription() {
        return Component.literal("Ignition control for solid fuel boosters.\n\n" +
            "In 1: Ignite (Value > 0)\n" +
            "Out 1: Fuel Ticks remaining\n" +
            "Out 2: Is Active (1 or 0)");
    }

    @Override
    public java.util.List<Component> getOutputNames() {
        return java.util.List.of(Component.literal("FUEL"), Component.literal("ON"));
    }

    @Override
    public int getInputCount() { return 1; }
    @Override
    public int getOutputCount() { return 2; }

    @Override
    public int renderCustomUI(net.minecraft.client.gui.GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        int barW = width - 20;
        int barH = 6;
        int bx = x + 10;
        int by = y + 10;

        // Background
        graphics.fill(bx, by, bx + barW, by + barH, 0x44000000);
        graphics.renderOutline(bx, by, barW, barH, 0x33FFFFFF);

        // Progress (assuming fuelTicks max is 200)
        double fuel = currentVal;
        float progress = (float)(fuel / 200.0);
        int fillW = (int)(barW * progress);
        
        if (fillW > 0) {
            graphics.fill(bx, by, bx + fillW, by + barH, 0xFFFFAA00);
        }

        return 20;
    }

    @Override
    public boolean isTrigger() { return true; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double ignite = context.evaluateInput(node.id, 0);
        
        var peripherals = context.getPeripherals();
        if (node.engineIndex >= 0 && node.engineIndex < peripherals.size()) {
            IPeripheral p = peripherals.get(node.engineIndex);
            if (p.getPeripheralType().equals("booster")) {
                if (ignite > 0.5) p.writeValue("thrust", 1.0);
                
                // If it's the second output pin requested (via some mechanism)
                // For now, evaluate always returns first output. 
                // Multi-output logic is handled in evaluate(node, pin) in NodeGraph.
                return p.readValue("fuel");
            }
        }
        
        return 0;
    }

    @Override
    public boolean isAvailable(NodeContext context) {
        return context.getPeripherals().stream().anyMatch(p -> p.getPeripheralType().equals("booster"));
    }

    @Override
    public String getIcon() { return "B"; }

    @Override
    public int getHeaderColor() { return 0xFFFFAA00; }
}
