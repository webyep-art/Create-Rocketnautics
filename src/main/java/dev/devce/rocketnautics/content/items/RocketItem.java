package dev.devce.rocketnautics.content.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RocketItem extends Item {
    public RocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.literal(" ")); 
            tooltipComponents.add(Component.translatable(this.getDescriptionId() + ".tooltip.shift").withStyle(ChatFormatting.GOLD));
        } else {
            tooltipComponents.add(Component.translatable("rocketnautics.tooltip.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
