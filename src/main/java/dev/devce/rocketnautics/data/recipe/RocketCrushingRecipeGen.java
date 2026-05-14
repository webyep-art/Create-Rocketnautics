package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.CrushingRecipeGen;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketBlocks;
import dev.devce.rocketnautics.registry.RocketItems;
import dev.devce.rocketnautics.registry.RocketTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

import static dev.devce.rocketnautics.registry.RocketBlocks.*;
import static dev.devce.rocketnautics.registry.RocketItems.*;
import static dev.devce.rocketnautics.registry.RocketTags.*;

public class RocketCrushingRecipeGen extends CrushingRecipeGen {

    GeneratedRecipe TITANIUM_ORE = stoneOre(RocketBlocks.TITANIUM_ORE::get, CRUSHED_TITANIUM::get, 1.75f, 350);
    GeneratedRecipe DEEP_TITANIUM_ORE = deepslateOre(DEEPSLATE_TITANIUM_ORE::get, CRUSHED_TITANIUM::get, 2.25f, 450);
    GeneratedRecipe RAW_TITANIUM_ORE = rawOre("titanium", MetalTags.TITANIUM::rawOres, CRUSHED_TITANIUM::get, 1);
    GeneratedRecipe RAW_TITANIUM_BLOCK = rawOreBlock("titanium", MetalTags.TITANIUM.rawStorageBlocks::items, CRUSHED_TITANIUM::get, 1);

    public RocketCrushingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }
}
