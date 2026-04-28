package dev.ryanhcode.sable.fabric.event;

import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when Sable's {@link dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem} is ticking physics.
 * </br>
 * Note that multiple physics ticks are completed per game tick, based on the amount of configured sub-steps.
 * Logic that needs to influence the physics world should occur on the physics tick, and not the game tick
 * due to this reason.
 */
@FunctionalInterface
public interface FabricSablePrePhysicsTickEvent extends SablePrePhysicsTickEvent {
    Event<SablePrePhysicsTickEvent> EVENT = EventFactory.createArrayBacked(SablePrePhysicsTickEvent.class, (events) -> (system, timeStep) -> {
        for (final SablePrePhysicsTickEvent event : events) {
            event.prePhysicsTick(system, timeStep);
        }
    });
}
