package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.api.peripherals.IPeripheral;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;

public class ThrusterNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Rocket Thruster"); }

    @Override
    public String getCategory() { return "Peripheral"; }

    @Override
    public Component getDescription() {
        return Component.literal("Controls the throttle of a simple engine.\n\n" +
            "Input (0.0 to 1.0) sets the thrust percentage.\n" +
            "Set the Engine Index (IDX) by clicking the node body.");
    }

    @Override
    public int getInputCount() { return 1; }

    @Override
    public boolean isTrigger() { return true; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double throttle = context.evaluateInput(node.id, 0);
        var peripherals = context.getPeripherals();
        
        if (node.engineIndex >= 0 && node.engineIndex < peripherals.size()) {
            IPeripheral p = peripherals.get(node.engineIndex);
            p.writeValue("throttle", throttle);
        }
        
        return throttle;
    }

    @Override
    public boolean isAvailable(NodeContext context) {
        return !context.getPeripherals().isEmpty();
    }

    @Override
    public String getIcon() { return "T"; }

    @Override
    public int getHeaderColor() { return 0xFF00FFFF; }
}
