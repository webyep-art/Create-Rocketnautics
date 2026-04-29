package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.blocks.RocketThrusterBlock;
import dev.devce.rocketnautics.content.blocks.VectorThrusterBlock;
import dev.devce.rocketnautics.content.blocks.BoosterThrusterBlock;
import dev.devce.rocketnautics.content.blocks.RCSThrusterBlock;
import dev.devce.rocketnautics.content.blocks.SeparatorBlock;
import dev.devce.rocketnautics.content.items.CreditsBookItem;
import dev.devce.rocketnautics.content.items.RocketItem;
import dev.devce.rocketnautics.content.items.RocketBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

public class RocketBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RocketNautics.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RocketNautics.MODID);

    public static final DeferredBlock<Block> ROCKET_THRUSTER = registerBlock("rocket_thruster",
            () -> new RocketThrusterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final DeferredBlock<Block> VECTOR_THRUSTER = registerBlock("vector_thruster",
            () -> new VectorThrusterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final DeferredBlock<Block> BOOSTER_THRUSTER = registerBlock("booster_thruster",
            () -> new BoosterThrusterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final DeferredBlock<Block> RCS_THRUSTER = registerBlock("rcs_thruster",
            () -> new RCSThrusterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final DeferredBlock<Block> SEPARATOR = registerBlock("separator",
            () -> new SeparatorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final DeferredItem<RocketItem> MUSIC_DISC_SPACE = ITEMS.register("music_disc_space",
            () -> new RocketItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, 
                            ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "brittle_rille")))));

    public static final DeferredItem<CreditsBookItem> CREDITS_BOOK = ITEMS.register("credits_book",
            () -> new CreditsBookItem(new Item.Properties().stacksTo(1)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ITEMS.register(name, () -> new RocketBlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
