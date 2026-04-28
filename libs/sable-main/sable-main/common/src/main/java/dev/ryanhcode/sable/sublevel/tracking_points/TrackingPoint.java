package dev.ryanhcode.sable.sublevel.tracking_points;

import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.UUID;

/**
 * @param globalPlaceholderPosition we store a global placeholder for tracking points that are made before a sub-level is
 *                                  saved, that is removed when it is moved & saved in the holding chunk map
 */
public record TrackingPoint(boolean inSubLevel, @Nullable UUID subLevelID, @Nullable GlobalSavedSubLevelPointer lastSavedSubLevelPointer, Vector3d point, Vector3d globalPlaceholderPosition) {
}
