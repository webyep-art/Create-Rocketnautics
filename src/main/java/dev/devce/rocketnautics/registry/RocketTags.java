package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class RocketTags {

    public static class Fluids {
        public static final TagKey<Fluid> ROCKET_FUEL = create("rocket_fuel");

        private static TagKey<Fluid> create(String name) {
            return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, name));
        }
    }
}
