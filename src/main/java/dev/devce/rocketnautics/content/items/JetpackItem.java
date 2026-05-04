package dev.devce.rocketnautics.content.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class JetpackItem extends ArmorItem {
    public JetpackItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, ArmorItem.Type.CHESTPLATE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.literal(" "));
            tooltipComponents.add(Component.translatable("item.rocketnautics.jetpack.tooltip.desc").withStyle(ChatFormatting.GOLD));
            tooltipComponents.add(Component.literal(" ")); 
            tooltipComponents.add(Component.translatable("item.rocketnautics.jetpack.tooltip.control").withStyle(ChatFormatting.AQUA));
        } else {
            tooltipComponents.add(Component.translatable("rocketnautics.tooltip.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
