package dev.ryanhcode.sable.neoforge.event;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.neoforged.bus.api.Event;

/**
 * Fired when Sable's {@link SubLevelPhysicsSystem} is complete with a physics tick.
 * </br>
 * Note that multiple physics ticks are completed per game tick, based on the amount of configured sub-steps.
 * Logic that needs to influence the physics world should occur on the physics tick, and not the game tick
 * due to this reason.
 */
public class ForgeSablePostPhysicsTickEvent extends Event {
    private final SubLevelPhysicsSystem physicsSystem;
    private final double timeStep;

    public ForgeSablePostPhysicsTickEvent(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        this.physicsSystem = physicsSystem;
        this.timeStep = timeStep;
    }

    public SubLevelPhysicsSystem getPhysicsSystem() {
        return this.physicsSystem;
    }

    public double getTimeStep() {
        return this.timeStep;
    }
}