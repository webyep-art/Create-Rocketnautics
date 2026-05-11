package dev.devce.websnodelib.api;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NodeRegistry {
    private static final Map<ResourceLocation, NodeFactory> REGISTRY = new HashMap<>();
    private static final Map<ResourceLocation, String> CATEGORIES = new HashMap<>();

    public static void register(ResourceLocation id, String category, NodeFactory factory) {
        REGISTRY.put(id, factory);
        CATEGORIES.put(id, category);
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

    public static String getCategory(ResourceLocation id) {
        return CATEGORIES.getOrDefault(id, "Other");
    }

    @FunctionalInterface
    public interface NodeFactory {
        WNode create(int x, int y);
    }
}
