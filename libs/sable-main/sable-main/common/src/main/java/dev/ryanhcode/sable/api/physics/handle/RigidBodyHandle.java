package dev.ryanhcode.sable.api.physics.handle;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A handle for easy access to physics-related operations on a {@link dev.ryanhcode.sable.sublevel.ServerSubLevel}.
 */
public class RigidBodyHandle {
    private final PhysicsPipelineBody body;
    private final SubLevelPhysicsSystem physicsSystem;

    /**
     * Obtains a handle for a given physics body.
     *
     * @param level the level to obtain the handle for
     * @param body  the sub-level to obtain the handle for
     */
    @Contract("_,_ -> _")
    public static @Nullable RigidBodyHandle of(final ServerLevel level, final PhysicsPipelineBody body) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

        if (container == null) {
            return null;
        }
        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

        return new RigidBodyHandle(body, physicsSystem);
    }

    /**
     * Obtains a handle for a given server sub-level.
     * </br>
     * If the physics system is already available in-scope or this is being called in bulk, the handle should be obtained
     * through {@link SubLevelPhysicsSystem#getPhysicsHandle(ServerSubLevel)}.
     *
     * @param subLevel the sub-level to obtain the handle for
     */
    @Contract("_ -> new")
    public static @Nullable RigidBodyHandle of(final ServerSubLevel subLevel) {
        final ServerLevel level = subLevel.getLevel();
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

        if (container == null) {
            return null;
        }

        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

        return physicsSystem.getPhysicsHandle(subLevel);
    }

    @ApiStatus.Internal
    public RigidBodyHandle(final PhysicsPipelineBody body, final SubLevelPhysicsSystem physicsSystem) {
        this.body = body;
        this.physicsSystem = physicsSystem;
    }

    /**
     * Adds a momenta impulse at a given world position to a data containing the position
     *
     * @param position the position inside the plot to apply the force at [m]
     * @param force    the local impulse to apply [N]
     */
    public void applyImpulseAtPoint(final Vector3dc position, final Vector3dc force) {
        this.physicsSystem.getPipeline().applyImpulse(this.body, position, force);
    }

    /**
     * Adds a momenta impulse at a given world position to a data containing the position
     *
     * @param position the position inside the plot to apply the force at [m]
     * @param force    the local impulse to apply [N]
     */
    public void applyImpulseAtPoint(final Vec3 position, final Vec3 force) {
        this.physicsSystem.getPipeline().applyImpulse(this.body, JOMLConversion.toJOML(position), JOMLConversion.toJOML(force));
    }

    /**
     * Adds to both local linear and angular momenta
     *
     * @param impulse the local impulse to apply [N]
     * @param torque  the local torque to apply [Nm]
     */
    public void applyLinearAndAngularImpulse(final Vector3dc impulse, final Vector3dc torque) {
        this.applyLinearAndAngularImpulse(impulse, torque, true);
    }

    /**
     * Adds to both local linear and angular momenta
     *
     * @param impulse the local impulse to apply [N]
     * @param torque  the local torque to apply [Nm]
     */
    public void applyLinearAndAngularImpulse(final Vector3dc impulse, final Vector3dc torque, final boolean wakeUp) {
        this.physicsSystem.getPipeline().applyLinearAndAngularImpulse(this.body, impulse, torque, wakeUp);
    }

    /**
     * Adds to local linear momenta
     *
     * @param impulse the local impulse to apply [N]
     */
    public void applyLinearImpulse(final Vector3dc impulse) {
        this.applyLinearAndAngularImpulse(impulse, JOMLConversion.ZERO);
    }

    /**
     * Adds to local angular momenta
     *
     * @param impulse the local impulse to apply [N]
     */
    public void applyAngularImpulse(final Vector3dc impulse) {
        this.applyLinearAndAngularImpulse(JOMLConversion.ZERO, impulse);
    }

    /**
     * Adds to local angular momenta
     *
     * @param torque the local torque to apply [Nm]
     */
    public void applyTorqueImpulse(final Vector3dc torque) {
        this.applyAngularImpulse(torque);
    }

    /**
     * @return the global linear velocity of the body from the physics engine [m/s]
     * @deprecated Use {@link RigidBodyHandle#getLinearVelocity(Vector3d)} instead.
     */
    @Deprecated
    public Vector3dc getLinearVelocity() {
        return this.physicsSystem.getPipeline().getLinearVelocity(this.body, new Vector3d());
    }

    /**
     * @return the global angular velocity of the body from the physics engine [rad/s]
     */
    @Deprecated
    public Vector3dc getAngularVelocity() {
        return this.physicsSystem.getPipeline().getAngularVelocity(this.body, new Vector3d());
    }

    /**
     * @param dest the destination vector to store the result in
     * @return the global linear velocity of the body from the physics engine, stored in dest [m/s]
     */
    public Vector3d getLinearVelocity(final Vector3d dest) {
        return this.physicsSystem.getPipeline().getLinearVelocity(this.body, dest);
    }

    /**
     * @param dest the destination vector to store the result in
     * @return the global angular velocity of the body from the physics engine, stored in dest [rad/s]
     */
    public Vector3d getAngularVelocity(final Vector3d dest) {
        return this.physicsSystem.getPipeline().getAngularVelocity(this.body, dest);
    }

    /**
     * Applies forces from a force applicator to this body.
     * If the forces have not changed significantly since the last time the force total was used, the rigid-body will not be woken up.
     */
    public void applyForcesAndReset(final ForceTotal forceTotal) {
        forceTotal.applyForces(this);
    }

    /**
     * Adds linear and angular velocities
     *
     * @param linearVelocity  the linear velocity to apply [m/s]
     * @param angularVelocity the angular velocity to apply [rad/s]
     */
    public void addLinearAndAngularVelocity(final Vector3dc linearVelocity, final Vector3dc angularVelocity) {
        this.physicsSystem.getPipeline().addLinearAndAngularVelocity(this.body, linearVelocity, angularVelocity);
    }

    /**
     * Teleports the physics pipeline body to a given position.
     *
     * @param position    the new position to teleport to
     * @param orientation the new orientation to teleport to
     */
    public void teleport(final Vector3dc position, final Quaterniondc orientation) {
        this.physicsSystem.getPipeline().teleport(this.body, position, orientation);
    }

    /**
     * Checks if this handle is valid.
     *
     * @return true if the handle is alid
     */
    public boolean isValid() {
        return !this.body.isRemoved();
    }
}
