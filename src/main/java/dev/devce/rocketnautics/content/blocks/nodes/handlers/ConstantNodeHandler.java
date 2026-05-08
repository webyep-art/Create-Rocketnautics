package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;

public class ConstantNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Constant"); }

    @Override
    public String getCategory() { return "Input"; }

    @Override
    public Component getDescription() {
        return Component.literal("Outputs a fixed numeric value.\n\n" +
            "Click the node body to enter a value manually.");
    }

    @Override
    public double evaluate(Node node, NodeContext context) {
        return node.value;
    }

    @Override
    public String getIcon() { return "#"; }

    @Override
    public int getHeaderColor() { return 0xFF00AAFF; }
}
