package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

import static dev.devce.rocketnautics.registry.RocketBlocks.*;
import static dev.devce.rocketnautics.registry.RocketItems.*;
import static dev.devce.rocketnautics.registry.RocketTags.*;

public class RocketMixingRecipeGen extends MixingRecipeGen {

    GeneratedRecipe TITANIUM_ALLOY = create("titanium_alloy", b -> b
            .require(MetalTags.TITANIUM.ingots)
            .require(MetalTags.TITANIUM.ingots)
            .require(MetalTags.TITANIUM.ingots)
            .require(Items.NETHERITE_SCRAP)
            .output(RocketItems.TITANIUM_ALLOY, 4)
            .requiresHeat(HeatCondition.SUPERHEATED));

    public RocketMixingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }
}
