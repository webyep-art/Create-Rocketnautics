package dev.devce.rocketnautics.registry;

import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.devce.rocketnautics.data.LimitedRegistrateDataProvider;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RocketRegistrate extends SimulatedRegistrate {
    // THIS IS WHY I HATE PRIVATE FIELDS
    protected LimitedRegistrateDataProvider actualProvider;

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

    @Override
    protected void onData(GatherDataEvent event) {
        actualProvider = new LimitedRegistrateDataProvider(this, getModid(), event);
        // more effort than it's worth, considering we support english and russian by default.
        actualProvider.disableProvider(ProviderType.LANG);
        // TODO move blockstates and item models to Registrate-powered datagen
        actualProvider.disableProvider(ProviderType.BLOCKSTATE);
        actualProvider.disableProvider(ProviderType.ITEM_MODEL);
        event.getGenerator().addProvider(true, actualProvider);
    }

    @Override
    public <P extends RegistrateProvider> Optional<P> getDataProvider(ProviderType<P> type) {
        RegistrateDataProvider provider = this.actualProvider;
        if (provider != null) {
            return provider.getSubProvider(type);
        }
        throw new IllegalStateException("Cannot get data provider before datagen is started");
    }
}
