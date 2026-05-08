package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.api.peripherals.IPeripheral;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;

public class RCSNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("RCS Control"); }

    @Override
    public String getCategory() { return "Control"; }

    @Override
    public Component getDescription() {
        return Component.literal("Controls Reaction Control System (RCS) thrusters.\n\n" +
            "Input > 0 activates the gas jet for stabilization.");
    }

    @Override
    public int getInputCount() { return 1; }

    @Override
    public boolean isTrigger() { return true; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double active = context.evaluateInput(node.id, 0);
        
        var peripherals = context.getPeripherals();
        if (node.engineIndex >= 0 && node.engineIndex < peripherals.size()) {
            IPeripheral p = peripherals.get(node.engineIndex);
            if (p.getPeripheralType().equals("rcs")) {
                p.writeValue("thrust", active > 0.5 ? 1.0 : 0.0);
            }
        }
        
        return active > 0.5 ? 1.0 : 0.0;
    }

    @Override
    public boolean isAvailable(NodeContext context) {
        return context.getPeripherals().stream().anyMatch(p -> p.getPeripheralType().equals("rcs"));
    }

    @Override
    public String getIcon() { return "R"; }

    @Override
    public int getHeaderColor() { return 0xFFCC00FF; }
}
