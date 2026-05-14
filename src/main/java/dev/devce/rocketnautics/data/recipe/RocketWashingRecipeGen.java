package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.items.RocketItem;
import dev.devce.rocketnautics.registry.RocketItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static dev.devce.rocketnautics.registry.RocketItems.*;

public class RocketWashingRecipeGen extends WashingRecipeGen {

    GeneratedRecipe CRUSHED_TITANIUM = crushedOreRocket(RocketItems.CRUSHED_TITANIUM, TITANIUM_NUGGET::get, () -> Items.NETHERITE_SCRAP, .05f);

    public RocketWashingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }

    public GeneratedRecipe crushedOreRocket(ItemEntry<RocketItem> crushed, Supplier<ItemLike> nugget, Supplier<ItemLike> secondary,
                                            float secondaryChance) {
        return create(crushed::get, b -> b.output(nugget.get(), 9)
                .output(secondaryChance, secondary.get(), 1));
    }
}
