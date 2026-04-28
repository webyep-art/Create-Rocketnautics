package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.BackendDebugFlags;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.component.HitboxComponent;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.mixin.compatibility.flywheel.LightStorageAccessor;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.BitSet;

public class SableFlywheelLightStorage extends LightStorage {
    private static final int INVALID_SECTION = -1;
    public static final int STATIC_SCENE_ID = 0;

    private final SableLightLut sableLut;
    private final Int2ObjectMap<Long2IntMap> scene2SectionArenaIndexMap;

    private final BitSet changed = new BitSet();
    private final LongSet updatedSections = new LongOpenHashSet();
    private boolean isDebugOn = false;

    @Nullable
    private LongSet requestedSections;

    public SableFlywheelLightStorage(final LevelAccessor level) {
        super(level);
        this.sableLut = new SableLightLut();
        this.scene2SectionArenaIndexMap = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public EffectVisual<?> visualize(final VisualizationContext ctx, final float partialTick) {
        return new SableFlywheelLightStorage.DebugVisual(ctx, partialTick);
    }

    @Override
    public <C> Plan<C> createFramePlan() {
        return SimplePlan.of(() -> {
            if (BackendDebugFlags.LIGHT_STORAGE_VIEW != this.isDebugOn) {
                final var visualizationManager = VisualizationManager.get(this.level());

                // Really should be non-null, but just in case.
                if (visualizationManager != null) {
                    if (BackendDebugFlags.LIGHT_STORAGE_VIEW) {
                        visualizationManager.effects()
                                .queueAdd(this);
                    } else {
                        visualizationManager.effects()
                                .queueRemove(this);
                    }
                }
                this.isDebugOn = BackendDebugFlags.LIGHT_STORAGE_VIEW;
            }

            if (this.updatedSections.isEmpty() && this.requestedSections == null) {
                return;
            }

            this.updateLightSections();
        });
    }

    /**
     * Set the set of requested sections.
     * <p> When set, this will be processed in the next frame plan. It may not be set every frame.
     *
     * @param sections The set of sections requested by the impl.
     */
    @Override
    public void sections(final LongSet sections) {
        this.requestedSections = sections;
    }

    private void updateLightSections() {
        this.removeUnusedSections();

        final ActiveSableCompanion helper = Sable.HELPER;
        final ClientLevel level = Minecraft.getInstance().level;
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(level);

        final Int2ObjectMap<LongSet> sectionsToCollect;
        if (this.requestedSections == null) {
            sectionsToCollect = new Int2ObjectOpenHashMap<>();
        } else {
            sectionsToCollect = new Int2ObjectOpenHashMap<>();

            for (final long section : this.requestedSections) {
                final SectionPos sectionPos = SectionPos.of(section);
                final SubLevel subLevel = helper.getContaining(level, sectionPos);

                int lightingSceneId = 0;

                if (subLevel instanceof final ClientSubLevel clientSubLevel) {
                    final int subLevelLightingSceneId = container.getLightingSceneId(clientSubLevel);

                    if (subLevelLightingSceneId != -1) {
                        lightingSceneId = subLevelLightingSceneId;
                    }
                }

                sectionsToCollect.computeIfAbsent(lightingSceneId, x -> new LongOpenHashSet())
                        .add(section);
            }
        }

        for (final int scene : this.scene2SectionArenaIndexMap.keySet()) {
            final Long2IntMap section2ArenaIndex = this.scene2SectionArenaIndexMap.get(scene);
            final LongSet longs = sectionsToCollect.get(scene);

            if (longs != null) {
                longs.removeAll(section2ArenaIndex.keySet());
            }
        }

        for (final long updatedSection : this.updatedSections) {
            for (int x = -1; x <= 1; ++x) {
                for (int y = -1; y <= 1; ++y) {
                    for (int z = -1; z <= 1; ++z) {
                        final long section = SectionPos.offset(updatedSection, x, y, z);

                        final SectionPos sectionPos = SectionPos.of(section);
                        final SubLevel subLevel = helper.getContaining(level, sectionPos);

                        if (subLevel instanceof final ClientSubLevel clientSubLevel) {
                            final int lightingSceneId = container.getLightingSceneId(clientSubLevel);

                            if (lightingSceneId != -1) {
                                final Long2IntMap map = this.scene2SectionArenaIndexMap.get(lightingSceneId);

                                if (map != null && map.containsKey(section)) {
                                    sectionsToCollect.computeIfAbsent(lightingSceneId, ignored -> new LongOpenHashSet()).add(section);
                                }

                                continue;
                            }
                        }

                        if (this.scene2SectionArenaIndexMap.values().stream().anyMatch(map -> map.containsKey(section))) {
                            sectionsToCollect.computeIfAbsent(0, ignored -> new LongOpenHashSet()).add(section);
                        }
                    }
                }
            }
        }

        for (final Int2ObjectMap.Entry<LongSet> entry : sectionsToCollect.int2ObjectEntrySet()) {
            final int scene = entry.getIntKey();
            final LongSet sections = entry.getValue();

            for (final long section : sections) {
                this.collectSection(scene, section);
            }
        }

        this.updatedSections.clear();
        this.requestedSections = null;
    }

    public void onLightUpdate(final long section) {
        this.updatedSections.add(section);
    }

    public void collectSection(final int scene, final long section) {
        final int index = this.indexForSection(scene, section);

        this.changed.set(index);

        final long ptr = this.arena.indexToPointer(index);

        // Zero it out first. This is basically free and makes it easier to handle missing sections later.
        MemoryUtil.memSet(ptr, 0, LightStorage.SECTION_SIZE_BYTES);

        ((LightStorageAccessor) this).getCollector().collectSection(ptr, section);
    }

    private int indexForSection(final int scene, long section) {
        final Long2IntMap map = this.scene2SectionArenaIndexMap.get(scene);
        int out = map != null ? map.get(section) : INVALID_SECTION;

        // Need to allocate.
        if (out == INVALID_SECTION) {
            out = this.arena.alloc();
            this.scene2SectionArenaIndexMap.computeIfAbsent(scene, (ignored) -> {
                final Long2IntOpenHashMap newMap = new Long2IntOpenHashMap();
                newMap.defaultReturnValue(INVALID_SECTION);
                return newMap;
            }).put(section, out);

            final SectionPos sectionPos = SectionPos.of(section);
            final SubLevel subLevel = Sable.HELPER.getContainingClient(sectionPos);

            if (subLevel != null) {
                final LevelPlot plot = subLevel.getPlot();
                final ChunkPos centerChunk = plot.getCenterChunk();
                section = SectionPos.asLong(
                        sectionPos.x() - centerChunk.x,
                        sectionPos.y(),
                        sectionPos.z() - centerChunk.z
                );
            }

            this.beginTrackingSection(scene, section, out);
        }
        return out;
    }

    private void removeUnusedSections() {
        if (this.requestedSections == null) {
            return;
        }

        final ClientLevel world = Minecraft.getInstance().level;

        boolean anyRemoved = false;

        for (final Int2ObjectMap.Entry<Long2IntMap> sceneEntry : this.scene2SectionArenaIndexMap.int2ObjectEntrySet()) {
            final int sceneId = sceneEntry.getIntKey();
            final Long2IntMap section2ArenaIndex = sceneEntry.getValue();

            final var entries = section2ArenaIndex.long2IntEntrySet();
            final var it = entries.iterator();
            while (it.hasNext()) {
                final var entry = it.next();
                final var section = entry.getLongKey();

                if (!this.requestedSections.contains(section)) {
                    this.arena.free(entry.getIntValue());

                    var localSection = section;
                    final SectionPos sectionPos = SectionPos.of(section);
                    final SubLevelContainer container = SubLevelContainer.getContainer(world);

                    if (container != null && container.inBounds(sectionPos.x(), sectionPos.z())) {
                        final int logPlotSize = container.getLogPlotSize();
                        final int plotX = (sectionPos.x() >> logPlotSize);
                        final int plotZ = (sectionPos.z() >> logPlotSize);

                        localSection = SectionPos.asLong(sectionPos.x() - ((plotX << logPlotSize) + (1 << (logPlotSize - 1))),
                                sectionPos.y(),
                                sectionPos.z() - ((plotZ << logPlotSize) + (1 << (logPlotSize - 1))));
                    }

                    this.endTrackingSection(sceneId, localSection);
                    it.remove();
                    anyRemoved = true;
                }
            }
        }

        if (anyRemoved) {
            this.sableLut.prune();
            ((LightStorageAccessor)this).setNeedsLutRebuild(true);
        }
    }

    private void beginTrackingSection(final int scene, final long section, final int index) {
        this.sableLut.add(scene, section, index);
        ((LightStorageAccessor)this).setNeedsLutRebuild(true);
    }

    private void endTrackingSection(final int scene, final long section) {
        this.sableLut.remove(scene, section);
        ((LightStorageAccessor)this).setNeedsLutRebuild(true);
    }

    @Override
    public void uploadChangedSections(final StagingBuffer staging, final int dstVbo) {
        for (int i = this.changed.nextSetBit(0); i >= 0; i = this.changed.nextSetBit(i + 1)) {
            staging.enqueueCopy(this.arena.indexToPointer(i), SECTION_SIZE_BYTES, dstVbo, (long) i * SECTION_SIZE_BYTES);
        }
        this.changed.clear();
    }

    @Override
    public void upload(final GlBuffer buffer) {
        if (this.changed.isEmpty()) {
            return;
        }

        buffer.upload(this.arena.indexToPointer(0), (long) this.arena.capacity() * SECTION_SIZE_BYTES);
        this.changed.clear();
    }

    @Override
    public IntArrayList createLut() {
        return this.sableLut.flatten();
    }

    public class DebugVisual implements EffectVisual<LightStorage>, SimpleDynamicVisual {

        private final InstanceRecycler<TransformedInstance> boxes;
        private final Vec3i renderOrigin;

        public DebugVisual(final VisualizationContext ctx, final float partialTick) {
            this.renderOrigin = ctx.renderOrigin();
            this.boxes = new InstanceRecycler<>(() -> ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, HitboxComponent.BOX_MODEL)
                    .createInstance());
        }

        @Override
        public void beginFrame(final Context ctx) {
            this.boxes.resetCount();

            this.setupSectionBoxes();
            this.setupLutRangeBoxes();

            this.boxes.discardExtra();
        }

        private void setupSectionBoxes() {
            for (final Int2ObjectMap.Entry<Long2IntMap> entry : SableFlywheelLightStorage.this.scene2SectionArenaIndexMap.int2ObjectEntrySet()) {
                final int sceneId = entry.getIntKey();
                final Long2IntMap section2ArenaIndex = entry.getValue();
                section2ArenaIndex.keySet()
                        .forEach(l -> {
                            final var x = SectionPos.x(l) * 16 - this.renderOrigin.getX();
                            final var y = SectionPos.y(l) * 16 - this.renderOrigin.getY();
                            final var z = SectionPos.z(l) * 16 - this.renderOrigin.getZ();

                            final var instance = this.boxes.get();

                            // Slightly smaller than a full 16x16x16 section to make it obvious which sections
                            // are actually represented when many are tiled next to each other.
                            instance.setIdentityTransform()
                                    .translate(x + 1, y + 1, z + 1)
                                    .scale(14)
                                    .color(255, 255, sceneId * 64)
                                    .light(LightTexture.FULL_BRIGHT)
                                    .setChanged();
                        });
            }

        }

        private void setupLutRangeBoxes() {
            final var first = SableFlywheelLightStorage.this.sableLut.indices;

            final var base1 = first.base();
            final var size1 = first.size();

            final float debug1 = base1 * 16 - this.renderOrigin.getY();

            float min2 = Float.POSITIVE_INFINITY;
            float max2 = Float.NEGATIVE_INFINITY;

            float min3 = Float.POSITIVE_INFINITY;
            float max3 = Float.NEGATIVE_INFINITY;

            for (int y = 0; y < size1; y++) {
                final var second = first.getRaw(y);

                if (second == null) {
                    continue;
                }

                final var base2 = second.base();
                final var size2 = second.size();

                final float y2 = (base1 + y) * 16 - this.renderOrigin.getY() + 7.5f;

                min2 = Math.min(min2, base2);
                max2 = Math.max(max2, base2 + size2);

                float minLocal3 = Float.POSITIVE_INFINITY;
                float maxLocal3 = Float.NEGATIVE_INFINITY;

                final float debug2 = base2 * 16 - this.renderOrigin.getX();

                for (int x = 0; x < size2; x++) {
                    final var third = second.getRaw(x);

                    if (third == null) {
                        continue;
                    }

                    final var base3 = third.base();
                    final var size3 = third.size();

                    final float x2 = (base2 + x) * 16 - this.renderOrigin.getX() + 7.5f;

                    min3 = Math.min(min3, base3);
                    max3 = Math.max(max3, base3 + size3);

                    minLocal3 = Math.min(minLocal3, base3);
                    maxLocal3 = Math.max(maxLocal3, base3 + size3);

                    final float debug3 = base3 * 16 - this.renderOrigin.getZ();

                    for (int z = 0; z < size3; z++) {
                        this.boxes.get()
                                .setIdentityTransform()
                                .translate(x2, y2, debug3)
                                .scale(1, 1, size3 * 16)
                                .color(0, 0, 255)
                                .light(LightTexture.FULL_BRIGHT)
                                .setChanged();
                    }
                }

                this.boxes.get()
                        .setIdentityTransform()
                        .translate(debug2, y2, minLocal3 * 16 - this.renderOrigin.getZ())
                        .scale(size2 * 16, 1, (maxLocal3 - minLocal3) * 16)
                        .color(255, 0, 0)
                        .light(LightTexture.FULL_BRIGHT)
                        .setChanged();
            }

            this.boxes.get()
                    .setIdentityTransform()
                    .translate(min2 * 16 - this.renderOrigin.getX(), debug1, min3 * 16 - this.renderOrigin.getZ())
                    .scale((max2 - min2) * 16, size1 * 16, (max3 - min3) * 16)
                    .color(0, 255, 0)
                    .light(LightTexture.FULL_BRIGHT)
                    .setChanged();
        }

        @Override
        public void update(final float partialTick) {

        }

        @Override
        public void delete() {
            this.boxes.delete();
        }
    }
}
