package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RocketTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RocketNautics.MODID);

    public static final Supplier<CreativeModeTab> ROCKET_TAB = CREATIVE_MODE_TABS.register("rocket_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rocketnautics.tab"))
                    .icon(() -> new ItemStack(RocketBlocks.ROCKET_THRUSTER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RocketBlocks.ROCKET_THRUSTER.get());
                        output.accept(RocketBlocks.VECTOR_THRUSTER.get());
                        output.accept(RocketBlocks.BOOSTER_THRUSTER.get());
                        output.accept(RocketBlocks.RCS_THRUSTER.get());
                        output.accept(RocketBlocks.SEPARATOR.get());
                        output.accept(RocketBlocks.MUSIC_DISC_SPACE.get());
                        ItemStack credits = new ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
                        credits.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.translatable("item.rocketnautics.credits_book").withStyle(net.minecraft.ChatFormatting.GOLD));
                        credits.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT, 
                            new net.minecraft.world.item.component.WrittenBookContent(
                                net.minecraft.server.network.Filterable.passThrough("Credits"),
                                "webyep",
                                0,
                                java.util.List.of(
                                    net.minecraft.server.network.Filterable.passThrough(
                                        Component.literal("")
                                            .append(Component.literal("   Cosmonautics\n\n").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD))
                                            .append(Component.literal("This book is dedicated\nto everyone who helped\nmake this project\npossible!\n\n").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC))
                                            .append(Component.literal("Thank you all!").withStyle(net.minecraft.ChatFormatting.DARK_RED))
                                    ),
                                    net.minecraft.server.network.Filterable.passThrough(
                                        Component.literal("")
                                            .append(Component.literal("      Credits\n\n").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD))
                                            .append(Component.literal("Maintainer: ").withStyle(net.minecraft.ChatFormatting.DARK_BLUE)).append(Component.literal("web\n"))
                                            .append(Component.literal("Contributor: ").withStyle(net.minecraft.ChatFormatting.DARK_BLUE)).append(Component.literal("SSnowly, M_W_K\n"))
                                            .append(Component.literal("Artist: ").withStyle(net.minecraft.ChatFormatting.DARK_BLUE)).append(Component.literal("LordAret, Mruranium\n"))
                                            .append(Component.literal("Ideas: ").withStyle(net.minecraft.ChatFormatting.DARK_GREEN)).append(Component.literal("6j, Harbinger\n"))
                                            .append(Component.literal("Testers: ").withStyle(net.minecraft.ChatFormatting.DARK_RED)).append(Component.literal("ABOBA, Kernos, MrFeddy\n"))
                                            .append(Component.literal("Music: ").withStyle(net.minecraft.ChatFormatting.DARK_AQUA)).append(Component.literal("Kevin Macleod"))
                                    )
                                ),
                                true
                            )
                        );
                        output.accept(credits);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
