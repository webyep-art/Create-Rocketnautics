package dev.ryanhcode.sable.sublevel.render.fancy;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import org.joml.Vector3dc;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL40C.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43C.glMultiDrawElementsIndirect;

public class FancySubLevelCommandBuilder implements NativeResource {

    private static final int MAX_SECTIONS = 16 * 1024 / Integer.BYTES; // Min uniform buffer size
    private static final int INDIRECT_COMMAND_SIZE = 5 * Integer.BYTES;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final StagingBuffer stagingBuffer;
    private final int commandBuffer;

    private final Deque<FancySubLevelSectionCompiler.RenderSection> sectionQueue;

    private int drawCount;

    public FancySubLevelCommandBuilder(final StagingBuffer stagingBuffer) {
        this.stagingBuffer = stagingBuffer;
        this.commandBuffer = GlStateManager._glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.commandBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, MAX_SECTIONS * INDIRECT_COMMAND_SIZE, GL_STREAM_DRAW);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        this.sectionQueue = new LinkedBlockingDeque<>();
    }

    private void flush() {
        if (this.drawCount > 0) {
            this.stagingBuffer.copy(this.commandBuffer, 0);
            glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_BYTE, 0L, this.drawCount, 0);
        }
        this.drawCount = 0;
    }

    public void setup() {
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.commandBuffer);
    }

    public void clear() {
        this.flush();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }

    @Override
    public void free() {
        GlStateManager._glDeleteBuffers(this.commandBuffer);
    }

    public void draw(final FancySubLevelRenderData data, final RenderType renderType, final int sectionX, final int sectionY, final int sectionZ) {
        for (final FancySubLevelSectionCompiler.RenderSection section : data.getOcclusionData().getVisibleSections()) {
            final SectionPos pos = section.getPos();
            final int dx = pos.getX() - sectionX;
            final int dy = pos.getY() - sectionY;
            final int dz = pos.getZ() - sectionZ;

            final FancySubLevelSectionCompiler.CompiledSection compiledSection = section.getCompiledSection();
            for (final Direction direction : DIRECTIONS) {
                final BucketRenderBuffer.Slice slice = compiledSection.get(renderType, direction);
                if (slice == null) {
                    continue;
                }

                // TODO replace with occlusion culling
                final int dot = direction.getStepX() * dx + direction.getStepY() * dy + direction.getStepZ() * dz;
                if (dot > 0) {
                    continue;
                }

                final long pointer = this.stagingBuffer.reserve(INDIRECT_COMMAND_SIZE);
                MemoryUtil.memPutInt(pointer, 6);
                MemoryUtil.memPutInt(pointer + 4, slice.length());
                MemoryUtil.memPutInt(pointer + 8, 0);
                MemoryUtil.memPutInt(pointer + 12, direction.get3DDataValue() * 4);
                MemoryUtil.memPutInt(pointer + 16, slice.offset());

                this.drawCount++;
                if (this.drawCount >= MAX_SECTIONS) {
                    this.flush();
                }
            }
        }

        this.flush();
        this.sectionQueue.clear();
    }

}
