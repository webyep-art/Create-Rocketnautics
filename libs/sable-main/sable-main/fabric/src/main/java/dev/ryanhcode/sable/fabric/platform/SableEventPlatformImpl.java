package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.fabric.event.FabricSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.fabric.event.FabricSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.fabric.event.FabricSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SableEventPlatformImpl implements SableEventPlatform {

    @Override
    public void onSubLevelContainerReady(final SableSubLevelContainerReadyEvent event) {
        FabricSableSubLevelContainerReadyEvent.EVENT.register(event);
    }

    @Override
    public void onPhysicsTick(final SablePrePhysicsTickEvent event) {
        FabricSablePrePhysicsTickEvent.EVENT.register(event);
    }

    @Override
    public void onPostPhysicsTick(final SablePostPhysicsTickEvent event) {
        FabricSablePostPhysicsTickEvent.EVENT.register(event);
    }

}
