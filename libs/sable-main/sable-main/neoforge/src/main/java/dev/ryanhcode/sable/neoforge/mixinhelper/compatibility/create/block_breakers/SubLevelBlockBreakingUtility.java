package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.block_breakers;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BiPredicate;

/**
 * Utility for blocks that break blocks on sub-levels.
 */
public class SubLevelBlockBreakingUtility {

	/**
	 * Finds the best candidate block for a drill / block breaking block-entity to try breaking.
	 */
	public static BlockPos findBreakingPos(final BiPredicate<BlockPos, BlockState> canBreak, @Nullable final SubLevel subLevel, final Level level, final Vec3 drillFacingVec, final Vec3 center, final BlockPos breakingPos) {
		// move box forward to line up with drill head
		final double scaleDown = 2.0 / 16.0;
		final BoundingBox3d localMiningBox = new BoundingBox3d(new AABB(center.x - 0.5,
				center.y - 0.5,
				center.z - 0.5,
				center.x + 0.5,
				center.y + 0.5,
				center.z + 0.5).inflate(-scaleDown).move(drillFacingVec.scale(12.0 / 16.0 - scaleDown)));

		final BoundingBox3d globalMiningBox = new BoundingBox3d(localMiningBox);

		if (subLevel != null) {
			globalMiningBox.transform(subLevel.logicalPose(), globalMiningBox);
		}

		final BoundingBox3i globalBlockMiningBox = new BoundingBox3i(globalMiningBox);
		final BoundingBox3d otherLocalMiningBox = new BoundingBox3d();
		final ObjectList<BlockPos> possiblyBreakableBlocks = new ObjectArrayList<>();

		collectBlocksInBounds(canBreak, level, BlockPos.containing(center), globalBlockMiningBox, possiblyBreakableBlocks);

		for (final SubLevel otherSubLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(globalMiningBox))) {
			if (subLevel == otherSubLevel) continue; // don't mine things on the same sublevel

			globalMiningBox.transformInverse(otherSubLevel.logicalPose(), otherLocalMiningBox);
			globalBlockMiningBox.set(otherLocalMiningBox);

			collectBlocksInBounds(canBreak, level, BlockPos.containing(center), globalBlockMiningBox, possiblyBreakableBlocks);
		}

		BlockPos closestPosition = breakingPos;
		double closestDistanceSqr = Double.MAX_VALUE;

		for (final BlockPos possiblyBreakableBlock : possiblyBreakableBlocks) {
			if (Sable.HELPER.getContaining(level, possiblyBreakableBlock) == subLevel) continue;

			final Vec3 blockCenter = Vec3.atCenterOf(possiblyBreakableBlock);
			final double distanceSqr = Sable.HELPER.distanceSquaredWithSubLevels(level, center, blockCenter);

			if (distanceSqr < closestDistanceSqr) {
				closestDistanceSqr = distanceSqr;
				closestPosition = possiblyBreakableBlock;
			}
		}

		return closestPosition;
	}

	@Unique
	private static void collectBlocksInBounds(final BiPredicate<BlockPos, BlockState> canBreak, final Level level, final BlockPos drillPos, final BoundingBox3i globalBlockMiningBox, final ObjectList<BlockPos> possiblyBreakableBlocks) {
		final BlockPos.MutableBlockPos globalBlockPos = new BlockPos.MutableBlockPos();

		for (int x = globalBlockMiningBox.minX(); x <= globalBlockMiningBox.maxX(); x++) {
			for (int z = globalBlockMiningBox.minZ(); z <= globalBlockMiningBox.maxZ(); z++) {
				for (int y = globalBlockMiningBox.minY(); y <= globalBlockMiningBox.maxY(); y++) {
					globalBlockPos.set(x, y, z);
					final BlockState globalBlockState = level.getBlockState(globalBlockPos);

					if (canBreak.test(globalBlockPos, globalBlockState)) {
						if (globalBlockPos.equals(drillPos)) {
							continue;
						}

						possiblyBreakableBlocks.add(globalBlockPos.immutable());
					}
				}
			}
		}
	}
}
