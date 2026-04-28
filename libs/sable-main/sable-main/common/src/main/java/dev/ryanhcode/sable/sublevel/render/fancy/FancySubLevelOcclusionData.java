package dev.ryanhcode.sable.sublevel.render.fancy;

import foundry.veil.api.client.render.CullFrustum;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.SectionPos;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FancySubLevelOcclusionData {

    private static final int MIN_OCCLUSION_COUNT = 3 * 3 * 3;

    private final FancySubLevelRenderData data;
    private final Vector3i cameraSection;
    private final Deque<FancySubLevelSectionCompiler.RenderSection> sectionQueue;
    private final List<FancySubLevelSectionCompiler.RenderSection> renderSections;
    private final List<FancySubLevelSectionCompiler.RenderSection> visibleSections;
    private final Map<RenderType, AtomicInteger> visibleSectionCount;
    private final Lock sectionLock = new ReentrantLock();

    public FancySubLevelOcclusionData(final FancySubLevelRenderData data) {
        this.data = data;
        this.cameraSection = new Vector3i();
        this.sectionQueue = new LinkedBlockingDeque<>();
        this.renderSections = new ObjectArrayList<>();
        this.visibleSections = new ObjectArrayList<>();
        this.visibleSectionCount = new Reference2ObjectArrayMap<>();
    }

    public void invalidate() {
        this.cameraSection.set(Integer.MAX_VALUE);
        this.renderSections.clear();
        this.visibleSections.clear();
        this.visibleSectionCount.clear();
    }

    private void addAll() {
        final Vector3ic origin = this.data.getChunkOrigin();
        final Vector3ic size = this.data.getSize();

        for (int z = 0; z < size.z(); z++) {
            for (int x = 0; x < size.x(); x++) {
                for (int y = 0; y < size.y(); y++) {
                    final FancySubLevelSectionCompiler.RenderSection section = this.data.getRenderSection(origin.x() + x, origin.y() + y, origin.z() + z);
                    if (section != null) {
                        this.addSection(section);
                    }
                }
            }
        }
    }

    public void update(final int sectionX, final int sectionY, final int sectionZ, final boolean smartCull, final CullFrustum cullFrustum) {
        try {
            this.sectionLock.lock();
            if (!this.cameraSection.equals(sectionX, sectionY, sectionZ)) {
                this.cameraSection.set(sectionX, sectionY, sectionZ);
                this.renderSections.clear();
                // TODO

                this.addAll();

//            final Vector3ic size = this.data.getSize();
//            if (size.x() * size.y() * size.z() < MIN_OCCLUSION_COUNT) {
//                this.addAll();
//                return;
//            }
//
//            final Vector3ic origin = this.data.getChunkOrigin();
//
//            final FancySubLevelSectionCompiler.RenderSection currentSection = data.getRenderSection(origin.x() + sectionX, origin.y() + sectionY, origin.z() + sectionZ);
//            if (currentSection != null) {
//                this.sectionQueue.add(currentSection);
//            }
//
//            if (this.sectionQueue.isEmpty()) {
//                return; // TODO
//            }
//
//            while (!this.sectionQueue.isEmpty()) {
//                final FancySubLevelSectionCompiler.RenderSection section = this.sectionQueue.poll();
//
//            }
            }

            final Matrix4d transform = this.data.getSubLevel().renderPose().bakeIntoMatrix(new Matrix4d());
            final Vector3d pos = new Vector3d();

            this.visibleSections.clear();
            for (final AtomicInteger value : this.visibleSectionCount.values()) {
                value.set(0);
            }

            for (final FancySubLevelSectionCompiler.RenderSection section : this.renderSections) {
                final SectionPos sectionPos = section.getPos();

                transform.transformPosition(sectionPos.minBlockX() + 8, sectionPos.minBlockY() + 8, sectionPos.minBlockZ() + 8, pos);
                if (cullFrustum.testSphere(pos, 14F)) {
                    this.visibleSections.add(section);

                    final FancySubLevelSectionCompiler.CompiledSection compiledSection = section.getCompiledSection();
                    for (final RenderType layer : compiledSection.getLayers()) {
                        this.visibleSectionCount.computeIfAbsent(layer, unused -> new AtomicInteger()).incrementAndGet();
                    }
                }
            }
        } finally {
            this.sectionLock.unlock();
        }
    }

    public void addSection(final FancySubLevelSectionCompiler.RenderSection renderSection) {
        try {
            this.sectionLock.lock();
            final FancySubLevelSectionCompiler.CompiledSection compiledSection = renderSection.getCompiledSection();
            if (compiledSection.hasNoRenderableLayers()) {
                this.renderSections.remove(renderSection);
                return;
            }

            if (!this.renderSections.contains(renderSection)) {
                this.renderSections.add(renderSection);
            }
        } finally {
            this.sectionLock.unlock();
        }
    }

    public List<FancySubLevelSectionCompiler.RenderSection> getVisibleSections() {
        return this.visibleSections;
    }

    public boolean hasLayer(final RenderType renderType) {
        final AtomicInteger count = this.visibleSectionCount.get(renderType);
        return count != null && count.get() > 0;
    }
}
