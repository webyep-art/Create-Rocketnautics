package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;

public class MathNodeHandler implements NodeHandler {
    @Override
    public Component getDisplayName() { return Component.literal("Math"); }

    @Override
    public String getCategory() { return "Logic"; }
    
    @Override
    public Component getDescription() {
        return Component.literal("Performs arithmetic or comparison operations between two inputs.\n\n" +
            "Supports: +, -, *, /, % (modulo), ^ (power), >, <, == (equals), != (not equals), >=, <=.\n\n" +
            "Click the center of the node to cycle operations.");
    }

    @Override
    public int getInputCount() { return 2; }

    @Override
    public double evaluate(Node node, NodeContext context) {
        double a = context.evaluateInput(node.id, 0);
        double b = context.evaluateInput(node.id, 1);
        
        return switch (node.operation) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> b != 0 ? a / b : 0;
            case ">" -> a > b ? 1 : 0;
            case "<" -> a < b ? 1 : 0;
            case "==" -> Math.abs(a - b) < 0.001 ? 1 : 0;
            case "!=" -> Math.abs(a - b) > 0.001 ? 1 : 0;
            case ">=" -> a >= b ? 1 : 0;
            case "<=" -> a <= b ? 1 : 0;
            case "%" -> b != 0 ? a % b : 0;
            case "^" -> Math.pow(a, b);
            default -> a;
        };
    }

    @Override
    public String getIcon() { return "±"; }

    @Override
    public int getHeaderColor() { return 0xFFFFAA00; }
}
