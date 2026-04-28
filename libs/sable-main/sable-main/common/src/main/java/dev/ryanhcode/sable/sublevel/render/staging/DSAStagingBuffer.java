package dev.ryanhcode.sable.sublevel.render.staging;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.opengl.GL30C.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL44C.GL_MAP_PERSISTENT_BIT;
import static org.lwjgl.opengl.GL45C.*;

@ApiStatus.Internal
public class DSAStagingBuffer extends StagingBuffer {

    private final long size;
    private final int buffer;
    private final long pointer;
    /**
     * The offset to the next location in the buffer data can be written to.
     */
    private long writePointer;
    /**
     * The size of the chunk of memory available to be written to from write pointer.
     */
    private long writeRegionSize;
    /**
     * A list of all regions ready to be flushed.
     */
    private final LongList flushRegions;
    private final List<FencedArea> fences;

    DSAStagingBuffer(final long size) {
        this.size = size;
        this.buffer = GlStateManager._glGenBuffers();
        glNamedBufferStorage(this.buffer, size, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_CLIENT_STORAGE_BIT);
        this.pointer = nglMapNamedBufferRange(this.buffer, 0, size, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_FLUSH_EXPLICIT_BIT);
        this.flushRegions = new LongArrayList();
        this.fences = new ObjectArrayList<>();

        this.writePointer = 0;
        this.writeRegionSize = size;
    }

    @Override
    public void updateFencedAreas() {
        if (this.fences.isEmpty()) {
            return;
        }

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer size = stack.mallocInt(1);

            final Iterator<FencedArea> iterator = this.fences.iterator();
            while (iterator.hasNext()) {
                final FencedArea area = iterator.next();
                final long fence = area.fence;

                final int status = glGetSynci(fence, GL_SYNC_STATUS, size);
                if (size.get(0) != 1) {
                    throw new IllegalStateException("Expected 1 value from fence");
                }

                if (status == GL_SIGNALED) {
                    glDeleteSync(fence);
                    iterator.remove();
                }
            }
        }
    }

    private long allocate(final long size) {
        final long pointer = this.pointer + this.writePointer;
        if (!this.flushRegions.isEmpty()) {
            final long offset = this.flushRegions.getLong(this.flushRegions.size() - 2);
            final long length = this.flushRegions.getLong(this.flushRegions.size() - 1);
            if (offset + length == this.writePointer) {
                // Since the elements are next to each other, just expand the
                // previous region so the merger doesn't have to work so hard
                this.flushRegions.set(this.flushRegions.size() - 1, length + size);
            } else {
                this.flushRegions.add(this.writePointer);
                this.flushRegions.add(size);
            }
        } else {
            this.flushRegions.add(this.writePointer);
            this.flushRegions.add(size);
        }
        this.writePointer += size;
        this.writeRegionSize -= size;
        return pointer;
    }

    @Override
    public long reserve(final long size) {
        if (this.writePointer + size >= this.writeRegionSize) {
            // Since there is no more space, flush the fences early
            this.updateFencedAreas();

            // No fences left, so just restart
            if (this.fences.isEmpty()) {
                this.writePointer = 0;
                this.writeRegionSize = this.size;
                return this.allocate(size);
            }

            // If there's space, then go after the fences
            // Otherwise, try to go to the start
            final FencedArea fence = this.fences.getLast();
            if (fence.offset + fence.length + size < this.size) {
                // Check if the region has already been written to
                long end = fence.offset + fence.length;
                for (int i = 0; i < this.flushRegions.size(); i += 2) {
                    final long offset = this.flushRegions.getLong(i);
                    final long length = this.flushRegions.getLong(this.flushRegions.size() - 1);
                    if (offset + length + size >= this.size) {
                        this.writePointer = 0;
                        this.writeRegionSize = this.fences.getFirst().offset;
                        return this.allocate(size);
                    } else if (offset + length > end) {
                        end = offset + length;
                    }
                }

                this.writePointer = end;
                this.writeRegionSize = this.size - this.writePointer;
                return this.allocate(size);
            }

            // Otherwise, just go to the start
            this.writePointer = 0;
            this.writeRegionSize = this.fences.getFirst().offset;
        }
        return this.allocate(size);
    }

    @Override
    public void copy(final int buffer, final long writeOffset) {
        if (this.flushRegions.isEmpty()) {
            return;
        }

        long writeRegionOffset = 0;
        long offset = this.flushRegions.getLong(0);
        long length = this.flushRegions.getLong(1);
        for (int i = 2; i < this.flushRegions.size(); i += 2) {
            final long regionOffset = this.flushRegions.getLong(i);
            final long regionLength = this.flushRegions.getLong(i + 1);
            if (offset + length == regionOffset) {
                length += regionLength;
            } else {
                glFlushMappedNamedBufferRange(this.buffer, offset, length);
                glCopyNamedBufferSubData(this.buffer, buffer, offset, writeRegionOffset + writeOffset, length);
                this.fences.add(new FencedArea(glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0), offset, length));
                writeRegionOffset += length;
                offset = regionOffset;
                length = regionLength;
            }
        }
        glFlushMappedNamedBufferRange(this.buffer, offset, length);
        glCopyNamedBufferSubData(this.buffer, buffer, offset, writeRegionOffset + writeOffset, length);
        this.fences.add(new FencedArea(glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0), offset, length));
        this.flushRegions.clear();

        Collections.sort(this.fences);
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public long getUsedSize() {
        return this.writePointer;
    }

    @Override
    public void free() {
        glUnmapNamedBuffer(this.buffer);
        glDeleteBuffers(this.buffer);
    }

    private record FencedArea(long fence, long offset, long length) implements Comparable<FencedArea> {
        @Override
        public int compareTo(@NotNull final DSAStagingBuffer.FencedArea o) {
            return Long.compare(this.offset, o.offset);
        }
    }
}
