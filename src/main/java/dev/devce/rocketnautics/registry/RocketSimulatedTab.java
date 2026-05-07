package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

public class RocketSimulatedTab {
    public static final ResourceLocation SECTION = ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "rocketnautics");

    public static void init() {
        
        register("rocket_thruster", RocketBlocks.ROCKET_THRUSTER);
        register("vector_thruster", RocketBlocks.VECTOR_THRUSTER);
        register("booster_thruster", RocketBlocks.BOOSTER_THRUSTER);
        register("rcs_thruster", RocketBlocks.RCS_THRUSTER);
        register("separator", RocketBlocks.SEPARATOR);
        register("sputnik", RocketBlocks.SPUTNIK);
        register("music_disc_space", RocketBlocks.MUSIC_DISC_SPACE);
        register("credits_book", RocketBlocks.CREDITS_BOOK);
        register("jetpack", RocketBlocks.JETPACK);
    }

    private static void register(String name, Supplier<? extends ItemLike> item) {
        SimulatedRegistrate.TAB_ITEMS.add(() -> item.get().asItem());
        
        SimulatedRegistrate.ITEM_TO_SECTION.put(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, name), SECTION);
    }
}
