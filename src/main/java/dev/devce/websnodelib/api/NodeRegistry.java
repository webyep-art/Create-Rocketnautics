package dev.devce.websnodelib.api;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NodeRegistry {
    private static final Map<ResourceLocation, NodeFactory> REGISTRY = new HashMap<>();

    public static void register(ResourceLocation id, NodeFactory factory) {
        REGISTRY.put(id, factory);
    }

    public static WNode createNode(ResourceLocation id, int x, int y) {
        NodeFactory factory = REGISTRY.get(id);
        if (factory != null) {
            return factory.create(x, y);
        }
        return null;
    }

    public static java.util.Set<ResourceLocation> getRegisteredTypes() {
        return REGISTRY.keySet();
    }

    public static Map<ResourceLocation, NodeFactory> getRegistry() {
        return REGISTRY;
    }

    @FunctionalInterface
    public interface NodeFactory {
        WNode create(int x, int y);
    }
}
