package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.content.blocks.SputnikBlockEntity;
import dev.devce.websnodelib.client.ui.WNodeScreen;
import dev.devce.rocketnautics.network.SputnikNodeSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class SputnikClientUI {
    public static void openNodeScreen(SputnikBlockEntity blockEntity) {
        Minecraft.getInstance().setScreen(new WNodeScreen(
            Component.literal("Flight Computer"), 
            blockEntity.graph,
            (tag) -> PacketDistributor.sendToServer(new SputnikNodeSyncPayload(blockEntity.getBlockPos(), tag)),
            null
        ));
    }
}
