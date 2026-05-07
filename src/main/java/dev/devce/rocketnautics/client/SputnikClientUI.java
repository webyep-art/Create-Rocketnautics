package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.content.blocks.SputnikBlockEntity;
import dev.devce.rocketnautics.content.blocks.nodes.ui.NodeScreen;
import net.minecraft.client.Minecraft;

public class SputnikClientUI {
    public static void openNodeScreen(SputnikBlockEntity blockEntity) {
        Minecraft.getInstance().setScreen(new NodeScreen(blockEntity.graph, blockEntity));
    }
}
