package dev.devce.rocketnautics.content.blocks.nodes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NodeGraph {
    public interface EvaluationContext {
        double getAltitude();
        double getVelocity();
        double getPitch();
        double getYaw();
        double getRoll();
        double getX();
        double getY();
        double getZ();
        double getEngineThrust(int index);
        void setEngineThrust(int index, double thrust);
        void setEngineGimbal(int index, double pitch, double yaw);
        int getEngineCount();
        net.minecraft.core.BlockPos getEnginePos(int index);
        net.minecraft.world.level.Level getLevel();
        net.minecraft.core.BlockPos getBlockPos();
        void setOutput(String side, int strength);
    }

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

    public void tick(EvaluationContext context) {
        cache.clear();
        for (Node node : nodes) {
            if (node.type == NodeType.OUTPUT) {
                double val = evaluate(node, context);
                // If it's a boolean 1.0, make it full redstone 15. 
                // Otherwise treat as analog 0-15.
                int strength = (val == 1.0) ? 15 : (int) Math.max(0, Math.min(15, val));
                context.setOutput(node.selectedSide, strength);
            }
            if (node.type == NodeType.LINK_OUTPUT) {
                double val = evaluate(node, context);
                LinkedSignalHandler.setSignal(context.getLevel(), node.freqStack1, node.freqStack2, context.getBlockPos(), val);
            }
            if (node.type == NodeType.THRUST_SET || node.type == NodeType.GIMBAL_SET) {
                evaluate(node, context);
            }
        }
    }

    public double evaluate(Node node, EvaluationContext context) {
        if (cache.containsKey(node.id)) return cache.get(node.id);

        double result = 0;
        switch (node.type) {
            case NUMBER_INPUT -> result = node.value;
            case ALTITUDE -> result = context.getAltitude();
            case VELOCITY -> result = context.getVelocity();
            case PITCH -> result = context.getPitch();
            case YAW -> result = context.getYaw();
            case ROLL -> result = context.getRoll();
            case POS_X -> result = context.getX();
            case POS_Y -> result = context.getY();
            case POS_Z -> result = context.getZ();
            case THRUST_GET -> result = context.getEngineThrust(node.engineIndex);
            case ENGINE_ID -> result = node.engineIndex;
            case PERIPHERAL_LIST -> result = 0;
            case THRUST_SET -> {
                double val = getInputValue(node.id, 0, context);
                context.setEngineThrust(node.engineIndex, val);
                result = val;
            }
            case GIMBAL_SET -> {
                double pitch = getInputValue(node.id, 0, context);
                double yaw = getInputValue(node.id, 1, context);
                context.setEngineGimbal(node.engineIndex, pitch, yaw);
                result = pitch;
            }
            case COMPARE -> {
                double a = getInputValue(node.id, 0, context);
                double b = getInputValue(node.id, 1, context);
                result = switch (node.operation) {
                    case ">" -> a > b ? 1 : 0;
                    case "<" -> a < b ? 1 : 0;
                    case "==" -> Math.abs(a - b) < 0.001 ? 1 : 0;
                    default -> 0;
                };
            }
            case LOGIC -> {
                double a = getInputValue(node.id, 0, context);
                double b = getInputValue(node.id, 1, context);
                result = switch (node.operation) {
                    case "AND" -> (a > 0.5 && b > 0.5) ? 1 : 0;
                    case "OR" -> (a > 0.5 || b > 0.5) ? 1 : 0;
                    default -> 0;
                };
            }
            case MATH -> {
                double a = getInputValue(node.id, 0, context);
                double b = getInputValue(node.id, 1, context);
                result = switch (node.operation) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> b != 0 ? a / b : 0;
                    case "%" -> b != 0 ? a % b : 0;
                    case "POW" -> Math.pow(a, b);
                    case "MIN" -> Math.min(a, b);
                    case "MAX" -> Math.max(a, b);
                    default -> 0;
                };
            }
            case ADVANCED -> {
                double a = getInputValue(node.id, 0, context);
                result = switch (node.operation) {
                    case "SIN" -> Math.sin(a);
                    case "COS" -> Math.cos(a);
                    case "ABS" -> Math.abs(a);
                    case "SQRT" -> Math.sqrt(a);
                    case "ROUND" -> Math.round(a);
                    case "FLOOR" -> Math.floor(a);
                    case "CEIL" -> Math.ceil(a);
                    default -> a;
                };
            }
            case MEMORY -> {
                double data = getInputValue(node.id, 0, context);
                double set = getInputValue(node.id, 1, context);
                double reset = getInputValue(node.id, 2, context);

                if (reset > 0.5) {
                    node.value = 0;
                } else if (set > 0.5) {
                    node.value = data;
                }
                result = node.value;
            }
            case LINK_INPUT -> {
                result = LinkedSignalHandler.getSignal(context.getLevel(), node.freqStack1, node.freqStack2, context.getBlockPos());
            }
            case LINK_OUTPUT -> result = getInputValue(node.id, 0, context);
            case OUTPUT -> result = getInputValue(node.id, 0, context);
        }

        cache.put(node.id, result);
        return result;
    }

    public double getInputValue(UUID nodeId, int pin, EvaluationContext context) {
        for (NodeConnection conn : connections) {
            if (conn.targetNode.equals(nodeId) && conn.targetPin == pin) {
                Node src = getNode(conn.sourceNode);
                if (src != null) return evaluate(src, context);
            }
        }
        return 0;
    }
}
