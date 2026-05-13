package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.api.data.recipe.PressingRecipeGen;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

import static dev.devce.rocketnautics.registry.RocketBlocks.*;
import static dev.devce.rocketnautics.registry.RocketItems.*;
import static dev.devce.rocketnautics.registry.RocketTags.*;

public class RocketPressingRecipeGen extends PressingRecipeGen {

    GeneratedRecipe TITANIUM = create("titanium_ingot", b -> b
            .require(MetalTags.TITANIUM.ingots)
            .output(TITANIUM_SHEET));

        GeneratedRecipe TITANIUM_ALLOY = create("titanium_alloy_ingot", b -> b
            .require(MetalTags.TITANIUM_ALLOY.ingots)
            .output(TITANIUM_ALLOY_SHEET));

    public RocketPressingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }
}
