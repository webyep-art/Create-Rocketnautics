package dev.ryanhcode.sable.sublevel.render.fancy;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.MeshData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelTextureCache;
import dev.ryanhcode.sable.sublevel.render.fancy.task.FancySubLevelTaskScheduler;
import dev.ryanhcode.sable.sublevel.render.fancy.task.SubLevelTask;
import dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer;
import foundry.veil.api.client.render.VeilRenderSystem;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3ic;
import org.lwjgl.system.NativeResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class FancySubLevelSectionCompiler implements SubLevelTask.MeshUploader, NativeResource {

    private final BucketRenderBuffer buffer;
    private final SubLevelTextureCache textureCache;
    private final SubLevelMeshBuilder meshBuilder;
    private final FancySubLevelTaskScheduler scheduler;

    public FancySubLevelSectionCompiler(final StagingBuffer stagingBuffer, final BlockRenderDispatcher blockRenderDispatcher, final BlockEntityRenderDispatcher blockEntityRenderDispatcher) {
        this.buffer = new BucketRenderBuffer(stagingBuffer);
        this.textureCache = new SubLevelTextureCache();
        this.meshBuilder = new SubLevelMeshBuilder(blockRenderDispatcher, blockEntityRenderDispatcher, this.textureCache);
        this.scheduler = new FancySubLevelTaskScheduler(this, Runtime.getRuntime().availableProcessors());
        this.scheduler.start();
    }

    @Override
    public CompletableFuture<BucketRenderBuffer.Slice[]> upload(final SubLevelMeshBuilder.QuadMesh mesh) {
        return CompletableFuture.supplyAsync(() -> {
            final IntList[] faces = mesh.getFaces();
            final BucketRenderBuffer.Slice[] slices = new BucketRenderBuffer.Slice[faces.length];
            for (int i = 0; i < faces.length; i++) {
                final IntList array = faces[i];
                if (!array.isEmpty()) {
                    final BucketRenderBuffer.Slice slice = this.buffer.allocate(array.size() * Integer.BYTES / BucketRenderBuffer.QUAD_SIZE);
                    slice.writeInt().put(array.toIntArray());
                    slice.flush();
                    slices[i] = slice;
                }
            }
            return slices;
        }, Minecraft.getInstance());
    }

    public BucketRenderBuffer getBuffer() {
        return this.buffer;
    }

    public SubLevelTextureCache getTextureCache() {
        return this.textureCache;
    }

    @Override
    public SubLevelMeshBuilder getMeshBuilder() {
        return this.meshBuilder;
    }

    public FancySubLevelTaskScheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public void free() {
        this.buffer.free();
        this.textureCache.free();
    }

    public static class RenderSection implements NativeResource {

        private final SectionPos pos;
        private final Vector3ic origin;
        private final AtomicReference<CompiledSection> compiledSection;
        private boolean dirty;
        private boolean dirtyFromPlayer;

        public RenderSection(final SectionPos pos, final Vector3ic origin) {
            this.pos = pos;
            this.origin = origin;
            this.compiledSection = new AtomicReference<>(CompiledSection.UNCOMPILED);
            this.dirty = true;
            this.dirtyFromPlayer = false;
        }

        public void setCompiledSection(final CompiledSection compiledSection) {
            final CompiledSection oldSection = this.compiledSection.getAndSet(compiledSection);
            if (oldSection != null) {
                // Make sure the old section waits until the next frame to be destroyed
                VeilRenderSystem.renderThreadExecutor().execute(oldSection::free);
            }
        }

        public void setDirty(final boolean playerChanged) {
            this.dirty = true;
            this.dirtyFromPlayer |= playerChanged;
        }

        public void setNotDirty() {
            this.dirty = false;
            this.dirtyFromPlayer = false;
        }

        public SectionPos getPos() {
            return this.pos;
        }

        public Vector3ic getOrigin() {
            return this.origin;
        }

        public CompiledSection getCompiledSection() {
            return this.compiledSection.get();
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public boolean isDirtyFromPlayer() {
            return this.dirtyFromPlayer;
        }

        @Override
        public void free() {
            this.compiledSection.getAndSet(CompiledSection.UNCOMPILED).free();
        }
    }

    public static class CompiledSection implements NativeResource {

        public static final CompiledSection UNCOMPILED = new CompiledSection() {
            public boolean facesCanSeeEachother(final Direction face, final Direction otherFace) {
                return false;
            }
        };
        public static final CompiledSection EMPTY = new CompiledSection() {
            public boolean facesCanSeeEachother(final Direction face, final Direction otherFace) {
                return true;
            }
        };

        private final Map<RenderType, BucketRenderBuffer.Slice[]> quadLayers = new Reference2ObjectArrayMap<>(RenderType.chunkBufferLayers().size());
        private final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
        private VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        private MeshData.SortState transparencyState;

        public static CompiledSection create(final SubLevelMeshBuilder.Results results, final SubLevelTask.MeshUploader meshUploader) {
            try (results) {
                final FancySubLevelSectionCompiler.CompiledSection compiledSection = new FancySubLevelSectionCompiler.CompiledSection();
                compiledSection.visibilitySet = results.visibilitySet;
                compiledSection.renderableBlockEntities.addAll(results.blockEntities);
                compiledSection.transparencyState = results.transparencyState;

                final List<CompletableFuture<?>> futures = new ArrayList<>(results.renderedQuadLayers.size());
                for (final Map.Entry<RenderType, SubLevelMeshBuilder.QuadMesh> entry : results.renderedQuadLayers.entrySet()) {
                    futures.add(meshUploader.upload(entry.getValue())
                            .thenAcceptAsync(slice -> compiledSection.quadLayers.put(entry.getKey(), slice), Minecraft.getInstance()));
                }

                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                return compiledSection;
            }
        }

        public Collection<RenderType> getLayers() {
            return this.quadLayers.keySet();
        }

        public boolean hasNoRenderableLayers() {
            return this.quadLayers.isEmpty();
        }

        public @Nullable BucketRenderBuffer.Slice get(final RenderType renderType, final Direction face) {
            final BucketRenderBuffer.Slice[] slices = this.quadLayers.get(renderType);
            return slices != null ? slices[face.get3DDataValue()] : null;
        }

        public boolean isEmpty(final RenderType renderType) {
            return !this.quadLayers.containsKey(renderType);
        }

        public List<BlockEntity> getRenderableBlockEntities() {
            return this.renderableBlockEntities;
        }

        public boolean facesCanSeeEachother(final Direction face, final Direction otherFace) {
            return this.visibilitySet.visibilityBetween(face, otherFace);
        }

        @Override
        public void free() {
            for (final BucketRenderBuffer.Slice[] value : this.quadLayers.values()) {
                for (final BucketRenderBuffer.Slice slice : value) {
                    if (slice != null) {
                        slice.free();
                    }
                }
            }
            this.quadLayers.clear();
        }
    }
}
