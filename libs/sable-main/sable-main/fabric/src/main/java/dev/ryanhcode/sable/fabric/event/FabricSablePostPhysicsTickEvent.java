package dev.ryanhcode.sable.fabric.event;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when Sable's {@link dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem} is complete with a physics tick.
 * </br>
 * Note that multiple physics ticks are completed per game tick, based on the amount of configured sub-steps.
 * Logic that needs to influence the physics world should occur on the physics tick, and not the game tick
 * due to this reason.
 */
@FunctionalInterface
public interface FabricSablePostPhysicsTickEvent extends SablePostPhysicsTickEvent {
    Event<SablePostPhysicsTickEvent> EVENT = EventFactory.createArrayBacked(SablePostPhysicsTickEvent.class, (events) -> (system, timeStep) -> {
        for (final SablePostPhysicsTickEvent event : events) {
            event.postPhysicsTick(system, timeStep);
        }
    });
}
