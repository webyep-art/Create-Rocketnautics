package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.items.RocketItem;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.core.component.DataComponents.MAX_STACK_SIZE;

public class RocketItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RocketNautics.MODID);

    public static final Supplier<ArmorItem> SPACE_HELMET = ITEMS.register("space_helmet", () -> new ArmorItem(
            ArmorMaterials.SPACESUIT_MATERIAL,
            ArmorItem.Type.HELMET,
            new Item.Properties().stacksTo(1)
    ));
    public static final Supplier<ArmorItem> SPACE_CHESTPLATE = ITEMS.register("space_chestplate", () -> new ArmorItem(
            ArmorMaterials.SPACESUIT_MATERIAL,
            ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1)
    ));



    public static final Supplier<ArmorItem> SPACE_LEGGINGS = ITEMS.register("space_leggings", () -> new ArmorItem(
            ArmorMaterials.SPACESUIT_MATERIAL,
            ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1)
    ));
    public static final Supplier<ArmorItem> SPACE_BOOTS = ITEMS.register("space_boots", () -> new ArmorItem(
            ArmorMaterials.SPACESUIT_MATERIAL,
            ArmorItem.Type.BOOTS,
            new Item.Properties().stacksTo(1)
    ));

    public static final DeferredItem<RocketItem> JETPACK_UPGRADE = ITEMS.register("jetpack_upgrade",
            () -> new RocketItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<RocketItem> TETHER = ITEMS.register("tether",
            () -> new RocketItem(new Item.Properties()));






    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
