package dev.devce.rocketnautics.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import com.tterrag.registrate.providers.RegistrateProvider;
import dev.devce.rocketnautics.RocketNautics;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.lang.reflect.Field;
import java.util.Map;


public class LimitedRegistrateDataProvider extends RegistrateDataProvider {
    public LimitedRegistrateDataProvider(AbstractRegistrate<?> parent, String modid, GatherDataEvent event) {
        super(parent, modid, event);
    }

    public void disableProvider(ProviderType<?> type) {
        // use reflection to remove the type from the private subProvider field
        try {
            Field subProvider = RegistrateDataProvider.class.getDeclaredField("subProviders");
            subProvider.setAccessible(true);
            Map<ProviderType<?>, RegistrateProvider> value = (Map<ProviderType<?>, RegistrateProvider>) subProvider.get(this);
            value.remove(type);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            RocketNautics.LOGGER.error("Failed to disable provider type {}", type);
        }
    }
}
