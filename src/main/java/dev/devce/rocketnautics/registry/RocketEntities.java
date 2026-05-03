package dev.devce.rocketnautics.registry;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.mobs.Starved;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RocketEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, RocketNautics.MODID);

    public static final Supplier<EntityType<Starved>> STARVED =
            ENTITY_TYPES.register("the_starved",
                    () -> EntityType.Builder.of(Starved::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .build("the_starved"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);

        eventBus.addListener(RocketEntities::registerAttributes);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RocketEntities.STARVED.get(), Starved.createAttributes().build());
    }
}