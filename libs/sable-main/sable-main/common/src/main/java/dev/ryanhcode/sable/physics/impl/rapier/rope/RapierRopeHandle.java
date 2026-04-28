package dev.ryanhcode.sable.physics.impl.rapier.rope;

import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

@ApiStatus.Internal
public record RapierRopeHandle(int sceneId, long handle) implements RopeHandle {

    public static RapierRopeHandle create(final int sceneId, final double pointRadius, final List<Vector3d> points) {
        final double[] coordinates = new double[points.size() * 3];

        for (int i = 0; i < points.size(); i++) {
            final Vector3d point = points.get(i);
            coordinates[i * 3] = point.x;
            coordinates[i * 3 + 1] = point.y;
            coordinates[i * 3 + 2] = point.z;
        }

        final long handle = Rapier3D.createRope(sceneId, pointRadius, points.get(0).distance(points.get(1)), coordinates, points.size());
        return new RapierRopeHandle(sceneId, handle);
    }

    /**
     * Queries the points of the rope from the physics engine
     */
    @Override
    public void readPose(final List<Vector3d> dest) {
        final double[] coordinates = Rapier3D.queryRope(this.sceneId, this.handle);
        for (int i = 0; i < coordinates.length; i += 3) {
            dest.get(i / 3).set(coordinates[i], coordinates[i + 1], coordinates[i + 2]);
        }
    }

    /**
     * Removes the rope from the physics pipeline
     */
    @Override
    public void remove() {
        Rapier3D.removeRope(this.sceneId, this.handle);
    }

    /**
     * Sets the extension constraint length of the first segment
     */
    @Override
    public void setFirstSegmentLength(final double length) {
        Rapier3D.setRopeFirstSegmentLength(this.sceneId, this.handle, length);
    }

    /**
     * Removes the point at the beginning of the rope
     */
    @Override
    public void removeFirstPoint() {
        Rapier3D.removeRopePointAtStart(this.sceneId, this.handle);
    }

    /**
     * Adds a point to the beginning of the rope
     */
    @Override
    public void addPoint(final Vector3dc position) {
        Rapier3D.addRopePointAtStart(this.sceneId, this.handle, position.x(), position.y(), position.z());
    }

    /**
     * Sets an attachment
     */
    @Override
    public void setAttachment(final AttachmentPoint attachmentPoint, final Vector3dc location, final ServerSubLevel subLevel) {
        Rapier3D.setRopeAttachment(this.sceneId, this.handle, subLevel == null ? -1 :  Rapier3D.getID(subLevel), location.x(), location.y(), location.z(), attachmentPoint == AttachmentPoint.END);
    }

    /**
     * Wakes up the rope
     */
    @Override
    public void wakeUp() {
        Rapier3D.wakeUpRope(this.sceneId, this.handle);
    }
}
