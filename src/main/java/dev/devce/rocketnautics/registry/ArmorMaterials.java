package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIAL = DeferredRegister.create(
            BuiltInRegistries.ARMOR_MATERIAL,

            RocketNautics.MODID
    );

    public static final Holder<ArmorMaterial> SPACESUIT_MATERIAL =
            ARMOR_MATERIAL.register("spacesuit", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 2);
                        map.put(ArmorItem.Type.LEGGINGS, 5);
                        map.put(ArmorItem.Type.CHESTPLATE, 6);
                        map.put(ArmorItem.Type.HELMET, 2);
                        map.put(ArmorItem.Type.BODY, 5);
                    }),
                    9,
                    SoundEvents.ARMOR_EQUIP_LEATHER,

                    () -> Ingredient.of(Tags.Items.INGOTS_IRON),

                    List.of(
                            new ArmorMaterial.Layer(
                                    ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "spacesuit")
                            ),

                            new ArmorMaterial.Layer(
                                    ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "copper"), "_overlay", true
                            )
                    ),

                    0,
                    0
            ));

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIAL.register(modEventBus);
    }
}
