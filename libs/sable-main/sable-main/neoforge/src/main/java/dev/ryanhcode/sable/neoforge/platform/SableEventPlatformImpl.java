package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SableEventPlatformImpl implements SableEventPlatform {

    @Override
    public void onSubLevelContainerReady(final SableSubLevelContainerReadyEvent event) {
        NeoForge.EVENT_BUS.<ForgeSableSubLevelContainerReadyEvent>addListener(forgeEvent -> event.onSubLevelContainerReady(forgeEvent.getLevel(), forgeEvent.getContainer()));
    }

    @Override
    public void onPhysicsTick(final SablePrePhysicsTickEvent event) {
        NeoForge.EVENT_BUS.<ForgeSablePrePhysicsTickEvent>addListener(forgeEvent -> event.prePhysicsTick(forgeEvent.getPhysicsSystem(), forgeEvent.getTimeStep()));
    }

    @Override
    public void onPostPhysicsTick(final SablePostPhysicsTickEvent event) {
        NeoForge.EVENT_BUS.<ForgeSablePostPhysicsTickEvent>addListener(forgeEvent -> event.postPhysicsTick(forgeEvent.getPhysicsSystem(), forgeEvent.getTimeStep()));
    }
}
