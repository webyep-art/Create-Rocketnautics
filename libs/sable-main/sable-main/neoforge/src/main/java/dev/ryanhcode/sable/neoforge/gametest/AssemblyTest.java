package dev.ryanhcode.sable.neoforge.gametest;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Sable.MOD_ID)
public final class AssemblyTest {

    @GameTest(template = "brittlebreak")
    public static void testBrittleBreaking(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(level);
        if (plotContainer == null) {
            throw new IllegalStateException("Plot container not found in level");
        }

        final SubLevelPhysicsSystem physicsSystem = plotContainer.physicsSystem();
        if (physicsSystem == null) {
            throw new IllegalStateException("Plot container does not have physics");
        }

        final BlockPos min = helper.absolutePos(new BlockPos(0, 1, 0));
        final BlockPos max = helper.absolutePos(new BlockPos(2, 3, 2));
        final BoundingBox3i bounds = new BoundingBox3i(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ()
        );

        final List<BlockState> expectedStates = new ArrayList<>(bounds.volume());
        for (final BlockPos pos : BlockPos.betweenClosed(min, max)) {
            expectedStates.add(level.getBlockState(pos));
        }

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, min, BlockPos.betweenClosed(min, max), bounds);
        physicsSystem.getPipeline().teleport(subLevel,
                new Vector3d(min.getX() + (1 + max.getX() - min.getX()) / 2.0,
                        min.getY() + (1 + max.getY() - min.getY()) / 2.0,
                        min.getZ() + (1 + max.getZ() - min.getZ()) / 2.0),
                helper.getTestRotation().rotation().transformation().getNormalizedRotation(new Quaterniond()));
        helper.runAtTickTime(10, () -> {
            final Level plot = subLevel.getLevel();
            final BoundingBox3ic sublevelBounds = subLevel.getPlot().getBoundingBox();
            final Vector3ic actualSize = sublevelBounds.size(new Vector3i());
            final Vector3ic expectedSize = bounds.size(new Vector3i());
            if (actualSize.equals(expectedSize)) {
                int i = 0;
                for (final BlockPos pos : BlockPos.betweenClosed(sublevelBounds.minX(),
                        sublevelBounds.minY(),
                        sublevelBounds.minZ(),
                        sublevelBounds.maxX(),
                        sublevelBounds.maxY(),
                        sublevelBounds.maxZ())) {
                    final BlockState expected = expectedStates.get(i);
                    if (!plot.getBlockState(pos).equals(expected)) {
                        throw new GameTestAssertPosException("Expected %s".formatted(expected.getBlock().getName().getString()), pos, pos, helper.getTick());
                    }
                    i++;
                }
                helper.succeed();
            } else {
                helper.fail("Expected %dx%dx%d region, got %dx%dx%d".formatted(
                        expectedSize.x(),
                        expectedSize.y(),
                        expectedSize.z(),
                        actualSize.x(),
                        actualSize.y(),
                        actualSize.z()));
            }
        });
    }
}
