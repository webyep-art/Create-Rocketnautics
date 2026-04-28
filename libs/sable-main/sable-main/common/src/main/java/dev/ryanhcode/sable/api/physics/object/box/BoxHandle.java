package dev.ryanhcode.sable.api.physics.object.box;

import dev.ryanhcode.sable.companion.math.Pose3d;
import org.jetbrains.annotations.ApiStatus;

/**
 * A handle to an active box in the physics engine.
 *
 * @see BoxPhysicsObject
 */
public interface BoxHandle {

    /**
     * Queries the pose of the box from the physics engine
     */
    @ApiStatus.OverrideOnly
    void readPose(Pose3d dest);

    /**
     * Removes the box from the physics pipeline
     */
    void remove();

    /**
     * Wakes up the box
     */
    void wakeUp();

    /**
     * @return the runtime ID of the box
     */
    int getRuntimeId();
}
