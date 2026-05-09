package dev.devce.rocketnautics.registry;

import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public class RocketRegistrate extends SimulatedRegistrate {

    public RocketRegistrate(ResourceLocation initialSection, String modId) {
        super(initialSection, modId);
    }

    @Override
    protected <R, T extends R> @NotNull RegistryEntry<R, T> accept(final String name, final ResourceKey<? extends Registry<R>> type, final Builder<R, T, ?, ?> builder, final NonNullSupplier<? extends T> creator, final NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        if (type.equals(Registries.ITEM)) {
            if (currentTab == null) {
                return super.accept(name, type, builder, creator, entryFactory);
            }
            // special handling: we need to prevent getting sent to the Aeronautics tab.
            var res = super.accept(name, type, builder, creator, entryFactory);
            // if the tab fails to pull an entry from this map, it just skips the item.
            ITEM_TO_SECTION.remove(res.getId());
            return res;
        } else {
            return super.accept(name, type, builder, creator, entryFactory);
        }
    }
}
