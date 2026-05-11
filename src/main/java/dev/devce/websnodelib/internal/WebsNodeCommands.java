package dev.devce.websnodelib.internal;

import com.mojang.brigadier.CommandDispatcher;
import dev.devce.websnodelib.api.WGraph;
import dev.devce.websnodelib.api.WNode;
import dev.devce.websnodelib.api.NodeRegistry;
import dev.devce.websnodelib.client.ui.WNodeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;

public class WebsNodeCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("webu")
            .then(Commands.literal("node_editor")
                .executes(context -> {
                    // Commands execute on server by default, but we want to open a UI
                    // For a demo command, we'll use a hack or assume it's called via a client-side event
                    // Actually, let's just provide the logic and the user can decide how to trigger it.
                    // In a real mod, you'd send a packet to the client.
                    
                    // For demonstration purposes, we will use Minecraft.getInstance() 
                    // which is CLIENT ONLY. This command should be registered on the client.
                    
                    Minecraft.getInstance().tell(() -> {
                        WGraph demoGraph = new WGraph();
                        
                        // Add some demo nodes
                        WNode mathNode = NodeRegistry.createNode(ResourceLocation.fromNamespaceAndPath("websnodelib", "math"), 100, 100);
                        WNode displayNode = NodeRegistry.createNode(ResourceLocation.fromNamespaceAndPath("websnodelib", "display"), 300, 150);
                        
                        if (mathNode != null) demoGraph.addNode(mathNode);
                        if (displayNode != null) demoGraph.addNode(displayNode);
                        
                        Minecraft.getInstance().setScreen(new WNodeScreen(demoGraph));
                    });
                    
                    return 1;
                })
            )
        );
    }
}
