package dev.ryanhcode.sable.api.physics.force;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Utility class for applying forces to {@link RigidBodyHandle rigid-body handles}
 */
public class ForceTotal {

    private final Vector3d temp = new Vector3d();
    private final Vector3d lastLocalForce = new Vector3d();
    private final Vector3d lastLocalTorque = new Vector3d();
    private final Vector3d localForce = new Vector3d();
    private final Vector3d localTorque = new Vector3d();

    @ApiStatus.Internal
    public void applyForces(final RigidBodyHandle handle) {
        final boolean forceChanged = this.localForce.distanceSquared( this.lastLocalForce) > 1e-5;
        final boolean torqueChanged = this.localTorque.distanceSquared(this.lastLocalTorque) > 1e-5;

        final boolean wakeUp = forceChanged || torqueChanged;
        handle.applyLinearAndAngularImpulse(this.localForce, this.localTorque, wakeUp);

        this.lastLocalForce.set(this.localForce);
        this.lastLocalTorque.set(this.localTorque);

        this.localForce.set(0.0, 0.0, 0.0);
        this.localTorque.set(0.0, 0.0, 0.0);
    }

    /**
     * Resets the current force total
     */
    public void reset() {
        this.localForce.set(0.0, 0.0, 0.0);
        this.localTorque.set(0.0, 0.0, 0.0);
    }

    /**
     * Applies another force total to this one
     *
     * @param other the other force total to apply
     */
    public void applyForceTotal(final ForceTotal other) {
        this.localForce.add(other.localForce);
        this.localTorque.add(other.localTorque);
    }

    /**
     * Adds to both local linear and angular momenta
     *
     * @param impulse the local impulse to apply [N]
     * @param torque  the local torque to apply [Nm]
     */
    public void applyLinearAndAngularImpulse(final Vector3dc impulse, final Vector3dc torque) {
        this.localForce.add(impulse);
        this.localTorque.add(torque);
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
     * Adds a momenta impulse at a given world position to a data containing the position
     *
     * @param position the position inside the plot to apply the force at [m]
     * @param force    the local impulse to apply [N]
     */
    public void applyImpulseAtPoint(final MassData massTracker, final Vector3dc position, final Vector3dc force) {
        this.localForce.add(force);
        position.sub(massTracker.getCenterOfMass(), this.temp);
        this.localTorque.add(this.temp.cross(force));
    }

    /**
     * Adds a momenta impulse at a given world position to a data containing the position
     *
     * @param position the position inside the plot to apply the force at [m]
     * @param force    the local impulse to apply [N]
     */
    public void applyImpulseAtPoint(final ServerSubLevel massTracker, final Vector3dc position, final Vector3dc force) {
        this.applyImpulseAtPoint(
                massTracker.getMassTracker(),
                position,
                force
        );
    }

    /**
     * @return the current totalled local force
     */
    public Vector3d getLocalForce() {
        return this.localForce;
    }

    /**
     * @return the current totalled local torque
     */
    public Vector3d getLocalTorque() {
        return this.localTorque;
    }

    /**
     * Adds a momenta impulse at a given world position to a data containing the position
     *
     * @param position the position inside the plot to apply the force at [m]
     * @param force    the local impulse to apply [N]
     */
    public void applyImpulseAtPoint(final MassTracker massTracker, final Vec3 position, final Vec3 force) {
        this.applyImpulseAtPoint(
                massTracker,
                JOMLConversion.toJOML(position),
                JOMLConversion.toJOML(force)
        );
    }
}
