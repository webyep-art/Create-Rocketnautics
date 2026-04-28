package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.Veil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.UUID;

public class FlywheelCompatNeoForge {
    public static boolean FLYWHEEL_LOADED = Veil.platform().isModLoaded("flywheel");

    private static final Long2ObjectMap<SubLevelFlwRenderState> RENDER_POSES = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    /**
     * Tries to add a flywheel visual for a block-entity.
     * Done for single block sub-level renderers, because we're not compiling their sections.
     */
    public static void tryAddVisual(final BlockEntity blockEntity) {
        VisualizationHelper.tryAddBlockEntity(blockEntity);
    }

    public static void preVisualizationFrame(final Level level, final float partialTicks) {
        final ClientSubLevelContainer container = (ClientSubLevelContainer) SubLevelContainer.getContainer(level);

        if (container == null) {
            RENDER_POSES.clear();
            return;
        }

        final ObjectIterator<Long2ObjectMap.Entry<SubLevelFlwRenderState>> iter = RENDER_POSES.long2ObjectEntrySet().iterator();

        while (iter.hasNext()) {
            final Long2ObjectMap.Entry<SubLevelFlwRenderState> entry = iter.next();
            final long pos = entry.getLongKey();
            final SubLevelFlwRenderState poseEntry = entry.getValue();

            final int plotX = ChunkPos.getX(pos);
            final int plotZ = ChunkPos.getZ(pos);

            final SubLevel subLevel = container.getSubLevel(plotX, plotZ);

            if (subLevel == null || !Objects.equals(subLevel.getUniqueId(), poseEntry.subLevelID)) {
                iter.remove();
                continue;
            }

            // TODO: some other form of removal when we don't have any more flywheel contraption visuals to worry about?

            updateEntry(container, (ClientSubLevel) subLevel, poseEntry, partialTicks);
        }
    }

    public static SubLevelFlwRenderState getInfo(final long plotCoord) {
        return RENDER_POSES.get(plotCoord);
    }

    private static void updateEntry(final ClientSubLevelContainer container, final ClientSubLevel clientSubLevel, final SubLevelFlwRenderState poseEntry, final float partialTicks) {
        poseEntry.sceneID = container.getLightingSceneId(clientSubLevel);
        poseEntry.subLevelID = clientSubLevel.getUniqueId();
        poseEntry.renderPose.set(clientSubLevel.renderPose(partialTicks));
        poseEntry.latestSkyLightScale = clientSubLevel.getLatestSkyLightScale();
        poseEntry.centerChunk = clientSubLevel.getPlot().getCenterChunk();
    }

    public static void createRenderInfo(final Level level, final SubLevel subLevel) {
        final ClientSubLevelContainer container = (ClientSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;

        final ChunkPos plotPos = subLevel.getPlot().plotPos;
        final long plotCoord = ChunkPos.asLong(plotPos.x - container.getOrigin().x, plotPos.z - container.getOrigin().y);

        RENDER_POSES.computeIfAbsent(plotCoord, x -> {
            final SubLevelFlwRenderState renderState = new SubLevelFlwRenderState();
            updateEntry(container, (ClientSubLevel) subLevel, renderState, 1.0f);
            return renderState;
        });
    }

    public static class SubLevelFlwRenderState {
        public int sceneID;
        public final Pose3d renderPose = new Pose3d();
        public UUID subLevelID;
        public float latestSkyLightScale;
        public ChunkPos centerChunk;
    }
}
