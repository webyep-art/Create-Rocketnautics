package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RocketSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RocketNautics.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_THRUST = SOUNDS.register("rocket_thrust",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "rocket_thrust")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BRITTLE_RILLE = SOUNDS.register("music.brittle_rille",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "music.brittle_rille")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_ARCADIA = SOUNDS.register("music.arcadia",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "music.arcadia")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
