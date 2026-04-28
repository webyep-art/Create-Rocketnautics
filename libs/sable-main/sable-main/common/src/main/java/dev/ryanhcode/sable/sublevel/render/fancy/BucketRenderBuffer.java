package dev.ryanhcode.sable.sublevel.render.fancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer;
import foundry.veil.api.client.render.vertex.VertexArray;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.IntBuffer;
import java.util.BitSet;

import static org.lwjgl.opengl.ARBCopyBuffer.*;
import static org.lwjgl.opengl.GL15C.*;

public class BucketRenderBuffer implements NativeResource {

    public static final int QUAD_SIZE = Integer.BYTES * 2;
    private static final int DEFAULT_MAX_QUADS = 1000;

    private final StagingBuffer stagingBuffer;
    private int buffer;
    private boolean dirty;

    private int size;
    private int maxSize;
    private BitSet closedBuckets;

    public BucketRenderBuffer(final StagingBuffer stagingBuffer) {
        this.stagingBuffer = stagingBuffer;
        this.buffer = GlStateManager._glGenBuffers();
        RenderSystem.glBindBuffer(GL_ARRAY_BUFFER, this.buffer);
        glBufferData(GL_ARRAY_BUFFER, DEFAULT_MAX_QUADS * QUAD_SIZE, GL_STREAM_DRAW);
        this.maxSize = DEFAULT_MAX_QUADS;
        this.closedBuckets = new BitSet(this.maxSize);
        this.dirty = true;
    }

    private void resize(final int newSize) {
        final int copyDest = GlStateManager._glGenBuffers();
        RenderSystem.glBindBuffer(GL_COPY_READ_BUFFER, this.buffer);
        RenderSystem.glBindBuffer(GL_COPY_WRITE_BUFFER, copyDest);
        glBufferData(GL_COPY_WRITE_BUFFER, (long) newSize * QUAD_SIZE, GL_STREAM_DRAW);
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, (long) this.maxSize * QUAD_SIZE);
        RenderSystem.glDeleteBuffers(this.buffer);
        this.buffer = copyDest;
        this.maxSize = newSize;
        final BitSet old = this.closedBuckets;
        this.closedBuckets = new BitSet(this.maxSize);
        this.closedBuckets.or(old);
        this.dirty = true;
    }

    public void clear() {
        this.size = 0;
        if (this.maxSize > DEFAULT_MAX_QUADS) {
            RenderSystem.glBindBuffer(GL_ARRAY_BUFFER, this.buffer);
            glBufferData(GL_ARRAY_BUFFER, DEFAULT_MAX_QUADS * QUAD_SIZE, GL_STREAM_DRAW);
            this.maxSize = DEFAULT_MAX_QUADS;
            this.closedBuckets = new BitSet(this.maxSize);
        } else {
            this.closedBuckets.clear();
        }
    }

    public Slice allocate(final int quadCount) {
        if (this.size + quadCount > this.maxSize) {
            this.resize((int) ((this.size + quadCount) * 1.5));
        }

        int fromIndex = this.closedBuckets.nextClearBit(0);
        int toIndex;
        while (true) {
            toIndex = this.closedBuckets.nextSetBit(fromIndex);
            if (toIndex == -1) {
                toIndex = fromIndex + quadCount - 1;
                break;
            }
            if (toIndex - fromIndex >= quadCount) {
                break;
            }
            fromIndex = this.closedBuckets.nextClearBit(toIndex);
        }

        if (toIndex >= this.maxSize) {
            this.resize((int) (toIndex * 1.5));
        }

        this.closedBuckets.set(fromIndex, toIndex + 1);
        this.size += quadCount;
        return new Slice(this, fromIndex, quadCount);
    }

    public void free(final Slice slice) {
        if (slice.closed) {
            return;
        }

        // The bucket is free to be used again
        this.closedBuckets.clear(slice.offset, slice.offset + slice.length);
        this.size -= slice.length;
        slice.closed = true;
//        Sable.LOGGER.info("Freed buffer from {} to {}", slice.offset, slice.offset + slice.length);
    }

    public void bind(final VertexArray vertexArray) {
        if (this.dirty) {
            this.dirty = false;
            vertexArray.editFormat().defineVertexBuffer(1, this.buffer, 0, QUAD_SIZE, 1);
        }
    }

    @Override
    public void free() {
        RenderSystem.glDeleteBuffers(this.buffer);
    }

    public static class Slice implements NativeResource {

        private final BucketRenderBuffer renderBuffer;
        private final int offset;
        private final int length;
        private boolean closed;

        private Slice(final BucketRenderBuffer renderBuffer, final int offset, final int length) {
            this.renderBuffer = renderBuffer;
            this.offset = offset;
            this.length = length;
        }

        public long write() {
            return this.renderBuffer.stagingBuffer.reserve((long) this.length * QUAD_SIZE);
        }

        public IntBuffer writeInt() {
            final long pointer = this.write();
            if ((pointer & 3) == 0) {
                return MemoryUtil.memIntBuffer(pointer, this.length * QUAD_SIZE / Integer.BYTES);
            }
            return MemoryUtil.memByteBuffer(pointer, this.length * QUAD_SIZE).asIntBuffer();
        }

        public void flush() {
            this.renderBuffer.stagingBuffer.copy(this.renderBuffer.buffer, (long) this.offset * QUAD_SIZE);
        }

        public int offset() {
            return this.offset;
        }

        public int length() {
            return this.length;
        }

        @Override
        public void free() {
            this.renderBuffer.free(this);
        }
    }
}
