package dev.devce.rocketnautics.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.RocketNauticsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RocketNauticsClientCommands {

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("rn")
            .then(Commands.literal("render")
                .then(Commands.literal("info")
                    .executes(context -> {
                        showRenderInfo();
                        return 1;
                    })
                )
            );
            
        dispatcher.register(builder);
    }

    private static void showRenderInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        RocketNauticsClient.showDebugOverlay = !RocketNauticsClient.showDebugOverlay;
        
        String status = RocketNauticsClient.showDebugOverlay ? "ENABLED" : "DISABLED";
        ChatFormatting color = RocketNauticsClient.showDebugOverlay ? ChatFormatting.GREEN : ChatFormatting.RED;
        
        mc.player.displayClientMessage(Component.literal("RocketNautics Render Debug Overlay: ")
            .append(Component.literal(status).withStyle(color)), true);
    }
}
