package dev.ryanhcode.sable.mixinhelpers;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.math.*;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.mixinterface.voxel_shape_iteration.FastVoxelShapeIterable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.Iterator;

public class CanFallAtleastHelper {

    public static Vector3d canFallAtleastWithSubLevels(final Level level, final AABB aabb) {
        final BoundingBox3d considerationBounds = new BoundingBox3d(aabb);
        considerationBounds.expand(1.05, considerationBounds); // fences

        final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(level, considerationBounds);

        final LevelReusedVectors sink = ((LevelExtension) level).sable$getJOMLSink();

        sink.entityBoxOrientation.identity();
        final OrientedBoundingBox3d entityBoundsOBB = new OrientedBoundingBox3d(
                (aabb.minX + aabb.maxX) / 2.0,
                (aabb.minY + aabb.maxY) / 2.0,
                (aabb.minZ + aabb.maxZ) / 2.0,
                aabb.getXsize() - 0.1,
                aabb.getYsize(),
                aabb.getZsize() - 0.1,
                sink.entityBoxOrientation,
                sink);


        final OrientedBoundingBox3d cubeOBB = new OrientedBoundingBox3d(sink);
        final BoundingBox3d localBounds = new BoundingBox3d();
        final Matrix4d bakedPose = new Matrix4d();

        final Vector3d center = new Vector3d();
        final Vector3d satResult = new Vector3d();

        for (final SubLevel subLevel : intersecting) {
            final Pose3dc pose = subLevel.lastPose();
            localBounds.set(aabb);
            localBounds.expand(-0.05, 0.0, -0.05, localBounds);
            localBounds.transformInverse(pose, bakedPose, localBounds);
            localBounds.minY -= 1.0; // fences

            final Iterable<BlockPos> blocks = BlockPos.betweenClosed(BlockPos.containing(localBounds.minX, localBounds.minY - 1, localBounds.minZ), BlockPos.containing(localBounds.maxX, localBounds.maxY, localBounds.maxZ));

            cubeOBB.getOrientation().set(pose.orientation());

            sink.entityBoxOrientation.identity().rotateY(SubLevelEntityCollision.getHitBoxYaw(pose));
            entityBoundsOBB.setOrientation(sink.entityBoxOrientation);

            for (final BlockPos block : blocks) {
                final BlockState state = level.getBlockState(block);
                final VoxelShape voxelShape = state.getCollisionShape(level, block);

                if (state.isAir()) {
                    continue;
                }

                final Iterator<BoundingBox3dc> iterator = ((FastVoxelShapeIterable) voxelShape).sable$allBoxes();
                while (iterator.hasNext()) {
                    final BoundingBox3dc box = iterator.next();
                    box.center(center);
                    cubeOBB.getPosition().set(block.getX() + center.x,
                            block.getY() + center.y,
                            block.getZ() + center.z);
                    pose.transformPosition(cubeOBB.getPosition());
                    box.size(cubeOBB.getDimensions());

                    OrientedBoundingBox3d.sat(entityBoundsOBB, cubeOBB, satResult);
                    if (satResult.lengthSquared() > 0 && satResult.x() != Double.MAX_VALUE && satResult.y() != Double.MAX_VALUE && satResult.z() != Double.MAX_VALUE) {
                        return satResult;
                    }
                }
            }
        }

        return null;
    }
}
