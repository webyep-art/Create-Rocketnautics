package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

import static dev.devce.rocketnautics.registry.RocketBlocks.*;
import static dev.devce.rocketnautics.registry.RocketItems.*;
import static dev.devce.rocketnautics.registry.RocketTags.*;

public final class RocketMechanicalCraftingRecipeGen extends MechanicalCraftingRecipeGen {

    GeneratedRecipe BOOSTER = create(BOOSTER_THRUSTER::get).returns(1).recipe(b -> b
            .patternLine("ABA")
            .patternLine("DCD")
            .patternLine("DFD")
            .patternLine("DED")
            .key('A', CommonMetal.IRON.plates)
            .key('B', CommonMetal.IRON.ingots)
            .key('C', MetalTags.TITANIUM.plates)
            .key('D', AllTags.AllItemTags.OBSIDIAN_PLATES.tag)
            .key('E', TITANIUM_NOZZLE)
            .key('F', MetalTags.TITANIUM.ingots)
            .disallowMirrored());

    GeneratedRecipe ROCKET = create(ROCKET_THRUSTER::get).returns(1).recipe(b -> b
            .patternLine("ABA")
            .patternLine(" C ")
            .patternLine("DED")
            .patternLine("DFD")
            .key('A', CommonMetal.IRON.plates)
            .key('B', CommonMetal.IRON.ingots)
            .key('C', MetalTags.TITANIUM.plates)
            .key('D', AllTags.AllItemTags.OBSIDIAN_PLATES.tag)
            .key('E', TITANIUM_NOZZLE)
            .key('F', MetalTags.TITANIUM.ingots)
            .disallowMirrored());

    public RocketMechanicalCraftingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }
}
