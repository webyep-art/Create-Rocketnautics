package dev.ryanhcode.sable.api.event;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

/**
 * Fired when Sable's {@link dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem} is ticking physics.
 * </br>
 * Note that multiple physics ticks are completed per game tick, based on the amount of configured sub-steps.
 * Logic that needs to influence the physics world should occur on the physics tick, and not the game tick
 * due to this reason.
 */
@FunctionalInterface
public interface SablePrePhysicsTickEvent {

    /**
     * Fired when Sable's {@link dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem} is ticking physics.
     *
     * @param physicsSystem the physics system running the physics tick
     * @param timeStep the time step of this physics tick [s]
     */
    void prePhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep);

}
