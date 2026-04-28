package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.fabric.event.FabricSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.fabric.event.FabricSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.fabric.event.FabricSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.platform.SableEventPublishPlatform;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SableEventPublishPlatformImpl implements SableEventPublishPlatform {

    /**
     * Called when a sub-level container is ready to use.
     *
     * @param level     The level instance
     * @param container The sub-level container that is ready
     */
    @Override
    public void onSubLevelContainerReady(final Level level, final SubLevelContainer container) {
        FabricSableSubLevelContainerReadyEvent.EVENT.invoker().onSubLevelContainerReady(level, container);
    }

    /**
     * Called when a sub-level container is ready to use.
     *
     * @param physicsSystem the physics system running the physics tick
     * @param timeStep      the time step of this physics tick [s]
     */
    @Override
    public void prePhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        FabricSablePrePhysicsTickEvent.EVENT.invoker().prePhysicsTick(physicsSystem, timeStep);
    }

    /**
     * Fired when Sable's {@link SubLevelPhysicsSystem} is complete with a physics tick.
     *
     * @param physicsSystem the physics system running the physics tick
     * @param timeStep      the time step of this physics tick [s]
     */
    @Override
    public void postPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        FabricSablePostPhysicsTickEvent.EVENT.invoker().postPhysicsTick(physicsSystem, timeStep);
    }
}
