package dev.ryanhcode.sable.mixinterface.voxel_shape_iteration;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;

@ApiStatus.Internal
public interface FastVoxelShapeIterable {

    Iterator<BoundingBox3dc> sable$allBoxes();
}
