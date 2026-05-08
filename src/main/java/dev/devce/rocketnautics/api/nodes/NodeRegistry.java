package dev.devce.rocketnautics.api.nodes;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public class NodeRegistry {
    public static final ResourceKey<Registry<NodeHandler>> KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "node_handlers"));
    public static final Registry<NodeHandler> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();
    
    public static final DeferredRegister<NodeHandler> NODE_HANDLERS = DeferredRegister.create(REGISTRY, RocketNautics.MODID);

    public static void register(IEventBus eventBus) {
        eventBus.addListener(NodeRegistry::onNewRegistry);
        NODE_HANDLERS.register(eventBus);
        // Force class loading of RocketNodes to trigger static field registration
        try {
            Class.forName("dev.devce.rocketnautics.registry.RocketNodes");
        } catch (ClassNotFoundException e) {
            RocketNautics.LOGGER.error("Failed to load RocketNodes", e);
        }
    }

    private static void onNewRegistry(net.neoforged.neoforge.registries.NewRegistryEvent event) {
        event.register(REGISTRY);
    }

    public static NodeHandler get(String id) {
        return REGISTRY.get(ResourceLocation.parse(id));
    }

    public static ResourceLocation getId(NodeHandler handler) {
        return REGISTRY.getKey(handler);
    }
}
