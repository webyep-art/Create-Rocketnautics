package dev.devce.rocketnautics.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class JetpackData {
    private static final String ROOT = "jetpack";

    private JetpackData() {
    }

    public static void enable(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag jetpack = new CompoundTag();
            jetpack.putBoolean("enabled", true);
            jetpack.putInt("fuel", 1000);
            jetpack.putInt("maxFuel", 1000);
            tag.put(ROOT, jetpack);
        });
    }

    public static boolean isEnabled(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }

        CompoundTag tag = data.copyTag();
        if (!tag.contains(ROOT)) {
            return false;
        }

        return tag.getCompound(ROOT).getBoolean("enabled");
    }

    public static int getFuel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }

        CompoundTag tag = data.copyTag();
        if (!tag.contains(ROOT)) {
            return 0;
        }

        return tag.getCompound(ROOT).getInt("fuel");
    }
}

