package dev.ryanhcode.sable.api.physics.object.rope;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

/**
 * A handle to an active rope in the physics engine.
 *
 * @see RopePhysicsObject
 */
public interface RopeHandle {

    /**
     * Queries the points of the rope from the physics engine
     */
    @ApiStatus.OverrideOnly
    void readPose(List<Vector3d> dest);

    /**
     * Removes the rope from the physics pipeline
     */
    void remove();

    /**
     * Sets the extension constraint length of the first segment
     */
    void setFirstSegmentLength(double length);

    /**
     * Removes the point at the beginning of the rope
     */
    void removeFirstPoint();

    /**
     * Adds a point to the beginning of the rope
     */
    void addPoint(final Vector3dc position);

    /**
     * Sets an attachment
     */
    void setAttachment(final AttachmentPoint attachmentPoint, final Vector3dc location, final ServerSubLevel subLevel);

    /**
     * Wakes up the rope
     */
    void wakeUp();

    /**
     * Rope attachment points
     */
    enum AttachmentPoint {
        START,
        END
    }

}
