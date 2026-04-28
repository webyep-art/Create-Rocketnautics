package dev.ryanhcode.sable.sublevel.render.sodium;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.mixinterface.sublevel_render.sodium.RenderSectionManagerExtension;
import dev.ryanhcode.sable.mixinterface.sublevel_render.sodium.SodiumWorldRendererExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.client.Camera;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.Iterator;

public class SodiumSubLevelRenderData implements SubLevelRenderData {


    public final Vector3d origin = new Vector3d();
    /**
     * The origin(minimum) of the render section grid
     */
    public final Vector3i chunkOrigin = new Vector3i();
    /**
     * The sub-level this renderer is for
     */
    private final ClientSubLevel subLevel;
    /**
     * The size of the render section grid
     */
    private final Vector3i size = new Vector3i();
    private boolean initialized = false;

    private final ObjectSet<SectionPos> newSections = new ObjectOpenHashSet<>();
    private final ObjectSet<SectionPos> visibleSections = new ObjectOpenHashSet<>();

    /**
     * Creates a new renderer for the given sub-level
     *
     * @param subLevel the sub-level to render
     */
    public SodiumSubLevelRenderData(final ClientSubLevel subLevel) {
        this.subLevel = subLevel;

        this.resize();
    }

    public void resize() {
        final SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instance();
        final SubLevelRenderSectionManager renderSectionManager = ((SodiumWorldRendererExtension) worldRenderer).sable$getSubLevelRenderSectionManager(this.subLevel);

        if (renderSectionManager == null) {
            return;
        }

        this.initialized = true;

        final BoundingBox3ic bounds = this.subLevel.getPlot().getBoundingBox();

        if (bounds != null && !bounds.equals(BoundingBox3i.EMPTY) && bounds.volume() > 0.0) {
            final Vector3i minChunkPos = new Vector3i(bounds.minX() >> 4, bounds.minY() >> 4, bounds.minZ() >> 4);
            final Vector3i maxChunkPos = new Vector3i(bounds.maxX() >> 4, bounds.maxY() >> 4, bounds.maxZ() >> 4);

            final Vector3i oldSize = new Vector3i(this.size);
            final Vector3i oldOrigin = new Vector3i(this.chunkOrigin);

            this.size.set(maxChunkPos.x() - minChunkPos.x() + 1, maxChunkPos.y() - minChunkPos.y() + 1, maxChunkPos.z() - minChunkPos.z() + 1);
            this.chunkOrigin.set(minChunkPos);
            this.origin.set(minChunkPos.x() << 4, minChunkPos.y() << 4, minChunkPos.z() << 4);


            final RenderSectionManagerExtension renderSectionManagerExtension = (RenderSectionManagerExtension) renderSectionManager;

            for (int x = minChunkPos.x(); x <= maxChunkPos.x(); x++) {
                for (int y = minChunkPos.y(); y <= maxChunkPos.y(); y++) {
                    for (int z = minChunkPos.z(); z <= maxChunkPos.z(); z++) {
                        if (!this.visibleSections.contains(SectionPos.of(x, y, z))) {
                            this.newSections.add(SectionPos.of(x, y, z));
                        }
                    }
                }
            }

//            ObjectIterator<SectionPos> iter = this.visibleSections.iterator();
//            while (iter.hasNext()) {
//                SectionPos pos = iter.next();
//                if (pos.x() < minChunkPos.x() || pos.x() > maxChunkPos.x() || pos.y() < minChunkPos.y() || pos.y() > maxChunkPos.y() || pos.z() < minChunkPos.z() || pos.z() > maxChunkPos.z()) {
//                    iter.remove();
//                }
//            }
        }
    }

    @Override
    public void rebuild() {
        // TODO
    }

    @Override
    public boolean isSectionCompiled(final int x, final int y, final int z) {
        // TODO
        return false;
    }

    @Override
    public void setDirty(final int x, final int y, final int z, final boolean playerChanged) {
        // TODO
    }

    @Override
    public void compileSections(final PrioritizeChunkUpdates chunkUpdates, final RenderRegionCache renderRegionCache, final Camera camera) {
        // TODO
    }

    @Override
    public ClientSubLevel getSubLevel() {
        return this.subLevel;
    }

    public void renderAdditional() {
        if (!this.initialized) {
            this.resize();
        }
    }

    public void updateChunks(final boolean updateChunksImmediately) {
        if (this.newSections.isEmpty()) {
            return;
        }


        final Level level = this.subLevel.getLevel();
        final LevelLightEngine lightEngine = level.getLightEngine();
        final Iterator<SectionPos> iterator = this.newSections.iterator();
        int count = 0;

        final SubLevelRenderSectionManager renderSectionManager = ((SodiumWorldRendererExtension) SodiumWorldRenderer.instance()).sable$getSubLevelRenderSectionManager(this.subLevel);

        final RenderSectionManagerExtension renderSectionManagerExtension = (RenderSectionManagerExtension) renderSectionManager;


        while (iterator.hasNext() && count < 1000) {
            final SectionPos newSection = iterator.next();
            if (lightEngine.lightOnInSection(newSection) && this.add(renderSectionManagerExtension, newSection, updateChunksImmediately)) {
//                iterator.remove();
            }
            count++;
        }

    }

    private boolean add(final RenderSectionManagerExtension manager, final SectionPos section, final boolean updateChunksImmediately) {
        RenderSection renderChunk = manager.sable$getRenderSection(section.x(), section.y(), section.z());

        if (renderChunk == null) {
            manager.sable$setRenderSectionDirty(section.x(), section.y(), section.z(), false);
            return false;
        }


        if (renderChunk.getOriginX() != section.origin().getX() || renderChunk.getOriginY() != section.origin().getY() || renderChunk.getOriginZ() != section.origin().getZ()) {
            manager.sable$setRenderSectionDirty(section.x(), section.y(), section.z(), false);
            renderChunk = manager.sable$getRenderSection(section.x(), section.y(), section.z());

            if (renderChunk == null || (renderChunk.getOriginX() != section.origin().getX() || renderChunk.getOriginY() != section.origin().getY() || renderChunk.getOriginZ() != section.origin().getZ())) {
                return false;
            }
        }

        manager.sable$setRenderSectionDirty(section.x(), section.y(), section.z(), true);
        return true;

    }

    @Override
    public void close() {
        ((SodiumWorldRendererExtension) SodiumWorldRenderer.instance()).sable$freeRenderSectionManager(this.subLevel);
    }
}
