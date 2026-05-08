package dev.devce.rocketnautics.content.blocks.nodes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.api.nodes.NodeRegistry;

public class NodeGraph {
    public final List<Node> nodes = new ArrayList<>();
    public final List<NodeConnection> connections = new ArrayList<>();
    private final Map<UUID, Double> cache = new HashMap<>();

    public NodeGraph() {}

    public void clearCache() {
        cache.clear();
    }

    public NodeGraph(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (tag.contains("Nodes")) {
            ListTag nodesTag = tag.getList("Nodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodesTag.size(); i++) {
                nodes.add(new Node(nodesTag.getCompound(i), registries));
            }
        }
        if (tag.contains("Connections")) {
            ListTag connTag = tag.getList("Connections", Tag.TAG_COMPOUND);
            for (int i = 0; i < connTag.size(); i++) {
                connections.add(new NodeConnection(connTag.getCompound(i)));
            }
        }
    }

    public CompoundTag save(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag nodesTag = new ListTag();
        for (Node n : nodes) {
            nodesTag.add(n.save(registries));
        }
        tag.put("Nodes", nodesTag);

        ListTag connTag = new ListTag();
        for (NodeConnection c : connections) {
            connTag.add(c.save());
        }
        tag.put("Connections", connTag);

        return tag;
    }

    public Node getNode(UUID id) {
        for (Node n : nodes) {
            if (n.id.equals(id)) return n;
        }
        return null;
    }

    public void tick(NodeContext context) {
        cache.clear();
        for (Node node : nodes) {
            NodeHandler handler = node.getHandler();
            if (handler != null && handler.isTrigger()) {
                evaluate(node, context);
            }
            
            // Legacy/Hardcoded triggers for now to keep it working during migration
            if (node.typeId.toString().endsWith("output")) {
                double val = evaluate(node, context);
                int strength = (val == 1.0) ? 15 : (int) Math.max(0, Math.min(15, val));
                context.setOutput(node.selectedSide, strength);
            }
            if (node.typeId.toString().endsWith("link_output")) {
                double val = evaluate(node, context);
                LinkedSignalHandler.setSignal(context.getLevel(), node.freqStack1, node.freqStack2, context.getBlockPos(), val);
            }
        }
    }

    public double evaluate(Node node, NodeContext context) {
        if (cache.containsKey(node.id)) return cache.get(node.id);

        double result = 0;
        NodeHandler handler = node.getHandler();
        if (handler != null) {
            result = handler.evaluate(node, context);
        } else {
            // Fallback for types not yet migrated
            result = fallbackEvaluate(node, context);
        }

        cache.put(node.id, result);
        return result;
    }

    private double fallbackEvaluate(Node node, NodeContext context) {
        String type = node.typeId.getPath().toUpperCase();
        return switch (type) {
            case "NUMBER_INPUT" -> node.value;
            case "COMPARE" -> {
                double a = context.evaluateInput(node.id, 0);
                double b = context.evaluateInput(node.id, 1);
                yield switch (node.operation) {
                    case ">" -> a > b ? 1 : 0;
                    case "<" -> a < b ? 1 : 0;
                    case "==" -> Math.abs(a - b) < 0.001 ? 1 : 0;
                    default -> 0;
                };
            }
            case "MATH" -> {
                double a = context.evaluateInput(node.id, 0);
                double b = context.evaluateInput(node.id, 1);
                yield switch (node.operation) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> b != 0 ? a / b : 0;
                    default -> 0;
                };
            }
            default -> 0;
        };
    }

    public double getInputValue(UUID nodeId, int pin, NodeContext context) {
        for (NodeConnection conn : connections) {
            if (conn.targetNode.equals(nodeId) && conn.targetPin == pin) {
                Node src = getNode(conn.sourceNode);
                if (src != null) return evaluate(src, context);
            }
        }
        return 0;
    }
}
