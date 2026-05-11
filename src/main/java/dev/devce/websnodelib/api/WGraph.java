package dev.devce.websnodelib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The core data structure for the node system.
 * A WGraph manages a collection of nodes and the connections between them.
 * It is responsible for logical updates (ticking) and data flow propagation.
 */
public class WGraph {
    private final List<WNode> nodes = new ArrayList<>();
    private final List<WConnection> connections = new ArrayList<>();

    /**
     * Adds a new node to the graph and recalculates the topological structure.
     * @param node The node instance to add.
     */
    public void addNode(WNode node) {
        node.setParentGraph(this);
        nodes.add(node);
        updateTopology();
    }

    /**
     * Removes a node and all its associated connections from the graph.
     * @param node The node to remove.
     */
    public void removeNode(WNode node) {
        nodes.remove(node);
        connections.removeIf(c -> c.sourceNode().equals(node.getId()) || c.targetNode().equals(node.getId()));
        updateTopology();
    }

    /**
     * Serializes the entire graph state into a NBT CompoundTag.
     * @return A tag containing all nodes, their internal data, and connections.
     */
    public net.minecraft.nbt.CompoundTag save() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        
        net.minecraft.nbt.ListTag nodesTag = new net.minecraft.nbt.ListTag();
        for (WNode node : nodes) nodesTag.add(node.save());
        tag.put("nodes", nodesTag);
        
        net.minecraft.nbt.ListTag connsTag = new net.minecraft.nbt.ListTag();
        for (WConnection conn : connections) {
            net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
            c.putString("src", conn.sourceNode().toString());
            c.putInt("srcP", conn.sourcePin());
            c.putString("tgt", conn.targetNode().toString());
            c.putInt("tgtP", conn.targetPin());
            connsTag.add(c);
        }
        tag.put("conns", connsTag);
        
        return tag;
    }

    /**
     * Reconstructs the graph state from a NBT CompoundTag.
     * @param tag The tag containing serialized graph data.
     */
    public void load(net.minecraft.nbt.CompoundTag tag) {
        nodes.clear();
        connections.clear();
        
        net.minecraft.nbt.ListTag nodesTag = tag.getList("nodes", 10);
        for (int i = 0; i < nodesTag.size(); i++) {
            net.minecraft.nbt.CompoundTag nTag = nodesTag.getCompound(i);
            net.minecraft.resources.ResourceLocation type = net.minecraft.resources.ResourceLocation.parse(nTag.getString("typeId"));
            WNode node = NodeRegistry.createNode(type, nTag.getInt("x"), nTag.getInt("y"));
            if (node != null) {
                node.load(nTag);
                addNode(node);
            }
        }
        
        net.minecraft.nbt.ListTag connsTag = tag.getList("conns", 10);
        for (int i = 0; i < connsTag.size(); i++) {
            net.minecraft.nbt.CompoundTag c = connsTag.getCompound(i);
            connect(java.util.UUID.fromString(c.getString("src")), c.getInt("srcP"), 
                    java.util.UUID.fromString(c.getString("tgt")), c.getInt("tgtP"));
        }
        updateTopology();
    }

    /**
     * Establishes a connection between an output pin of a source node and an input pin of a target node.
     * @param sourceNode UUID of the source node.
     * @param sourcePin Index of the output pin.
     * @param targetNode UUID of the target node.
     * @param targetPin Index of the input pin.
     */
    public void connect(UUID sourceNode, int sourcePin, UUID targetNode, int targetPin) {
        connections.add(new WConnection(sourceNode, sourcePin, targetNode, targetPin));
        updateTopology();
    }

    /**
     * @return An unmodifiable view of all nodes currently in the graph.
     */
    public List<WNode> getNodes() {
        return nodes;
    }

    private Object context;

    public void setContext(Object context) {
        this.context = context;
    }

    public Object getContext() {
        return context;
    }

    /**
     * Performs one logical step of the graph simulation.
     * 1. Propagates values across all connections.
     * 2. Evaluates the logic of each node.
     */
    public void tick() {
        // 1. Propagate data across connections
        for (WConnection conn : connections) {
            WNode source = findNode(conn.sourceNode());
            WNode target = findNode(conn.targetNode());
            if (source != null && target != null) {
                double val = source.getOutputs().get(conn.sourcePin()).getValue();
                target.getInputs().get(conn.targetPin()).setValue(val);
                target.getInputs().get(conn.targetPin()).setConnected(true);
                source.getOutputs().get(conn.sourcePin()).setConnected(true);
            }
        }

        // 2. Evaluate nodes
        for (WNode node : nodes) {
            node.evaluate();
        }
    }

    /**
     * Internal helper to find a node by its unique identifier.
     * @param id UUID of the node.
     * @return The node instance or null if not found.
     */
    private WNode findNode(UUID id) {
        return nodes.stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Updates the topological structure of the graph.
     * Calculates the "depth" of each node starting from root nodes (nodes with no inputs).
     * This depth is used for animation synchronization and processing order.
     */
    public void updateTopology() {
        // Reset depths
        for (WNode node : nodes) node.setTopoDepth(-1);
        
        java.util.Queue<WNode> queue = new java.util.LinkedList<>();
        
        // Find roots (nodes with no connected inputs)
        for (WNode node : nodes) {
            boolean hasInputs = false;
            for (WConnection conn : connections) {
                if (conn.targetNode().equals(node.getId())) {
                    hasInputs = true;
                    break;
                }
            }
            if (!hasInputs) {
                node.setTopoDepth(0);
                queue.add(node);
            }
        }
        
        // BFS to propagate depth, limited to prevent infinite loops in cycles
        int maxDepth = nodes.size();
        while (!queue.isEmpty()) {
            WNode current = queue.poll();
            int nextDepth = current.getTopoDepth() + 1;
            
            if (nextDepth > maxDepth) continue; // Cycle detected, stop propagating here
            
            for (WConnection conn : connections) {
                if (conn.sourceNode().equals(current.getId())) {
                    WNode target = findNode(conn.targetNode());
                    if (target != null && (target.getTopoDepth() == -1 || target.getTopoDepth() < nextDepth)) {
                        target.setTopoDepth(nextDepth);
                        queue.add(target);
                    }
                }
            }
        }
        
        // Handle remaining nodes (those in cycles with no external roots)
        for (WNode node : nodes) {
            if (node.getTopoDepth() == -1) node.setTopoDepth(0);
        }
    }

    /**
     * @return A list of all connections in the graph.
     */
    public List<WConnection> getConnections() {
        return connections;
    }
}
