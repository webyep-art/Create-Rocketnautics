package dev.ryanhcode.sable.sublevel.tracking_points;

import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SubLevelTrackingPointObserver implements SubLevelObserver {
    private final ServerLevel serverLevel;

    public SubLevelTrackingPointObserver(final ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
    }

    /**
     * Called before a sub-level is removed from a {@link dev.ryanhcode.sable.api.sublevel.SubLevelContainer}.
     *
     * @param subLevel the sub-level that will be removed
     */
    @Override
    public void onSubLevelRemoved(final SubLevel subLevel, final SubLevelRemovalReason reason) {
        if (reason == SubLevelRemovalReason.REMOVED) {
            final SubLevelTrackingPointSavedData data = this.getTrackingPointData();

            final List<UUID> toProject = getTrackingPoints((ServerSubLevel) subLevel, data);

            for (final UUID uuid : toProject) {
                final TrackingPoint trackingPoint = data.getTrackingPoint(uuid);

                if (trackingPoint != null) {
                    final Vector3dc point = subLevel.logicalPose().transformPosition(trackingPoint.point());
                    data.setTrackingPoint(uuid, new TrackingPoint(false, null, null, new Vector3d(point), null));
                }
            }
        }
    }

    private static @NotNull List<UUID> getTrackingPoints(final ServerSubLevel subLevel, final SubLevelTrackingPointSavedData data) {
        final List<UUID> toProject = new ObjectArrayList<>();

        for (final Map.Entry<UUID, TrackingPoint> entry : data.getAllTrackingPoints()) {
            final TrackingPoint trackingPoint = entry.getValue();

            final GlobalSavedSubLevelPointer pointer = trackingPoint.lastSavedSubLevelPointer();
            if (trackingPoint.inSubLevel() && pointer != null && pointer.equals(subLevel.getLastSerializationPointer())) {
                toProject.add(entry.getKey());
            }
        }
        return toProject;
    }

    private SubLevelTrackingPointSavedData getTrackingPointData() {
        return SubLevelTrackingPointSavedData.getOrLoad(this.serverLevel);
    }

}
