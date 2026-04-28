package dev.ryanhcode.sable.sublevel.render.staging;

import foundry.veil.api.client.render.VeilRenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.NativeResource;

import java.util.function.LongFunction;

public abstract class StagingBuffer implements NativeResource {

    private static StagingBufferType stagingBufferType;

    public static StagingBuffer create() {
        return create(16L * 1024L * 1024L);
    }

    public static StagingBuffer create(final long size) {
        if (stagingBufferType == null) {
            final GLCapabilities caps = GL.getCapabilities();
            if (caps.OpenGL44 || caps.GL_ARB_buffer_storage) {
                stagingBufferType = VeilRenderSystem.directStateAccessSupported() ? StagingBufferType.DSA : StagingBufferType.ARB;
            } else {
                stagingBufferType = StagingBufferType.LEGACY;
            }
        }
        return stagingBufferType.factory.apply(size);
    }

    /**
     * Attempts to free space in the buffer by checking the status of existing syncs.
     */
    public abstract void updateFencedAreas();

    /**
     * Queues the specified data to be written to the staging buffer for a future copy.
     *
     * @param size The size of the region to allocate
     * @return A pointer data can be written to
     */
    public abstract long reserve(final long size);

    /**
     * Copies the current region of the buffer into the specified buffer.
     *
     * @param buffer The buffer to copy into
     * @param offset The offset into the buffer to copy at
     */
    public abstract void copy(final int buffer, long offset);

    /**
     * @return The total size of the buffer
     */
    public abstract long getSize();

    /**
     * @return The size of the used portion of the buffer
     */
    public abstract long getUsedSize();

    private enum StagingBufferType {
        LEGACY(DSAStagingBuffer::new),
        ARB(DSAStagingBuffer::new),
        DSA(DSAStagingBuffer::new);

        private final LongFunction<StagingBuffer> factory;

        StagingBufferType(final LongFunction<StagingBuffer> factory) {
            this.factory = factory;
        }
    }
}
