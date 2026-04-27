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
                                    net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal("Maintainer: webyep\nArtist: LordAret\nContributor: SSnowly")),
                                    net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal("Inspiration:\n- Create Propulsion\n- Kerbal Space Program")),
                                    net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal("Testers:\n- GlemFeddy\n- ABOBA\n- Kernos")),
                                    net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal("Ideas:\n- Solid Fuel: Harbinger of Confusion\n- Fuel Pump System: 6j")),
                                    net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal("Assets:\n- Textures from Create Big Cannons\n- Music: Kevin Macleod (CC BY 4.0)"))
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
