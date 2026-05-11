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
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        dev.devce.rocketnautics.content.RocketTooltipHelper.appendTooltip(stack, this, tooltipComponents, tooltipFlag);
    }
}
