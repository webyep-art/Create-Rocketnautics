package dev.devce.rocketnautics.content;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public class RocketTooltipHelper {

    public static void appendTooltip(ItemStack stack, ItemLike item, List<Component> tooltip, TooltipFlag flag) {
        String baseKey = item.asItem().getDescriptionId();
        
        // 1. Funny/Main description (Always shown)
        String funnyKey = baseKey + ".funny";
        MutableComponent funny = Component.translatable(funnyKey);
        if (!funny.getString().equals(funnyKey)) {
            tooltip.add(funny.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        // 2. Shift for Summary logic
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("rocketnautics.tooltip.summary_title").withStyle(ChatFormatting.GOLD));
            
            String summaryKey = baseKey + ".summary";
            MutableComponent summary = Component.translatable(summaryKey);
            if (!summary.getString().equals(summaryKey)) {
                // Split summary into lines by '\n' if present in lang file
                tooltip.add(summary.withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Hold [").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("Shift").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("] for Summary").withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
