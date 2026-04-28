package dev.ryanhcode.sable.sublevel.render.dispatcher;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.sky_light_shadow.SableDynamicSkyLightShadowPreProcessor;
import dev.ryanhcode.sable.render.sky_light_shadow.SableSkyLightShadows;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelCommandBuilder;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelOcclusionData;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelSectionCompiler;
import dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.api.client.render.vertex.VertexArrayBuilder;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL20C.*;

public class FancySubLevelRenderDispatcher implements SubLevelRenderDispatcher {

    private static final Matrix4f TRANSFORM = new Matrix4f();
    private static final int VERTEX_SIZE = 8;

    private final Map<String, CompletableFuture<ShaderProgram>> dynamicPrograms;
    private final StagingBuffer stagingBuffer;
    private final FancySubLevelSectionCompiler sectionCompiler;
    private final FancySubLevelCommandBuilder commandBuilder;
    private final VertexArray vertexArray;

    public FancySubLevelRenderDispatcher() {
        this.dynamicPrograms = new Object2ObjectArrayMap<>();

        this.stagingBuffer = StagingBuffer.create();
        this.sectionCompiler = new FancySubLevelSectionCompiler(this.stagingBuffer, Minecraft.getInstance().getBlockRenderer(), Minecraft.getInstance().getBlockEntityRenderDispatcher());
        this.commandBuilder = new FancySubLevelCommandBuilder(this.stagingBuffer);

        this.vertexArray = VertexArray.create();
        final int vbo = this.vertexArray.getOrCreateBuffer(VertexArray.VERTEX_BUFFER);

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final ByteBuffer buffer = stack.malloc(6 * 4 * VERTEX_SIZE);
            // Down
            buffer.put((byte) 0).put((byte) 0).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) -1).put((byte) 0).put((byte) 0);
            buffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) -1).put((byte) 0).put((byte) 0);
            buffer.put((byte) 1).put((byte) 0).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) -1).put((byte) 0).put((byte) 0);
            buffer.put((byte) 1).put((byte) 0).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) -1).put((byte) 0).put((byte) 0);
            // Up
            buffer.put((byte) 0).put((byte) 1).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) 1).put((byte) 0).put((byte) 0);
            buffer.put((byte) 0).put((byte) 1).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) 1).put((byte) 0).put((byte) 0);
            buffer.put((byte) 1).put((byte) 1).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) 1).put((byte) 0).put((byte) 0);
            buffer.put((byte) 1).put((byte) 1).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) 1).put((byte) 0).put((byte) 0);
            // North
            buffer.put((byte) 1).put((byte) 1).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) -1).put((byte) 0);
            buffer.put((byte) 1).put((byte) 0).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) -1).put((byte) 0);
            buffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) -1).put((byte) 0);
            buffer.put((byte) 0).put((byte) 1).put((byte) 0).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) -1).put((byte) 0);
            // South
            buffer.put((byte) 0).put((byte) 1).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) 1).put((byte) 0);
            buffer.put((byte) 0).put((byte) 0).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) 1).put((byte) 0);
            buffer.put((byte) 1).put((byte) 0).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) 1).put((byte) 0);
            buffer.put((byte) 1).put((byte) 1).put((byte) 1).put((byte) 0)
                    .put((byte) 0).put((byte) 0).put((byte) 1).put((byte) 0);
            // West
            buffer.put((byte) 0).put((byte) 1).put((byte) 0).put((byte) 0)
                    .put((byte) -1).put((byte) 0).put((byte) 0).put((byte) 0);
            buffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0)
                    .put((byte) -1).put((byte) 0).put((byte) 0).put((byte) 0);
            buffer.put((byte) 0).put((byte) 0).put((byte) 1).put((byte) 0)
                    .put((byte) -1).put((byte) 0).put((byte) 0).put((byte) 0);
            buffer.put((byte) 0).put((byte) 1).put((byte) 1).put((byte) 0)
                    .put((byte) -1).put((byte) 0).put((byte) 0).put((byte) 0);
            // East
            buffer.put((byte) 1).put((byte) 1).put((byte) 1).put((byte) 0)
                    .put((byte) 1).put((byte) 0).put((byte) 0).put((byte) 0);
            buffer.put((byte) 1).put((byte) 0).put((byte) 1).put((byte) 0)
                    .put((byte) 1).put((byte) 0).put((byte) 0).put((byte) 0);
            buffer.put((byte) 1).put((byte) 0).put((byte) 0).put((byte) 0)
                    .put((byte) 1).put((byte) 0).put((byte) 0).put((byte) 0);
            buffer.put((byte) 1).put((byte) 1).put((byte) 0).put((byte) 0)
                    .put((byte) 1).put((byte) 0).put((byte) 0).put((byte) 0);
            buffer.flip();

            final ByteBuffer indices = stack.bytes((byte) 0, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 0);

            VertexArray.upload(vbo, buffer, VertexArray.DrawUsage.STATIC);
            this.vertexArray.uploadIndexBuffer(indices, VertexArray.IndexType.BYTE);
        }

        this.vertexArray.editFormat()
                .defineVertexBuffer(0, vbo, 0, VERTEX_SIZE, 0)
                .setVertexAttribute(0, 0, 3, VertexArrayBuilder.DataType.BYTE, false, 0)
                .setVertexAttribute(1, 0, 3, VertexArrayBuilder.DataType.BYTE, false, 4)
                .setVertexIAttribute(2, 1, 2, VertexArrayBuilder.DataType.UNSIGNED_INT, 0);
    }

    @Override
    public void onResourceManagerReload(final ResourceManager resourceManager) {
        this.freePrograms();
    }

    @Override
    public SubLevelRenderData resize(final ClientSubLevel subLevel, final SubLevelRenderData renderData) {
        ((FancySubLevelRenderData) renderData).resize();
        return renderData;
    }

    @Override
    public SubLevelRenderData createRenderData(final ClientSubLevel subLevel) {
        return new FancySubLevelRenderData(subLevel, this.sectionCompiler);
    }

    private @Nullable ShaderProgram getDynamicProgram(final ShaderInstance vanillaProgram) {
        final String name = VanillaShaderCompiler.getActiveDynamicBuffers(vanillaProgram) + "/" + vanillaProgram.getName();
        final CompletableFuture<ShaderProgram> future = this.dynamicPrograms.get(name);
        if (future != null) {
            return future.getNow(null);
        }

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final int size = glGetProgrami(vanillaProgram.getId(), GL_ATTACHED_SHADERS);
            final Int2ObjectMap<String> sources = new Int2ObjectArrayMap<>(size);
            final IntBuffer shaders = stack.mallocInt(size);
            glGetAttachedShaders(vanillaProgram.getId(), null, shaders);
            for (int i = 0; i < shaders.limit(); i++) {
                final int shader = shaders.get(i);
                final int type = glGetShaderi(shader, GL_SHADER_TYPE);
                sources.put(type, glGetShaderSource(shader));
            }

            this.dynamicPrograms.put(name, VeilRenderSystem.renderer().getShaderManager().createDynamicProgram(Sable.sablePath("dynamic_sublevel/" + name), sources)
                    .thenApplyAsync(shader -> {
                        final ShaderUniform sableEnableNormalLighting = shader.getUniform("SableEnableNormalLighting");
                        if (sableEnableNormalLighting != null) {
                            sableEnableNormalLighting.setFloat(1.0F);
                        }

                        final ShaderUniform sableEnableSkyLightShadows = shader.getUniform(SableDynamicSkyLightShadowPreProcessor.ENABLE_UNIFORM);
                        if (sableEnableSkyLightShadows != null) {
                            sableEnableSkyLightShadows.setFloat(SableSkyLightShadows.isEnabled() ? 1.0F : 0.0F);
                        }

                        return shader;
                    }, Minecraft.getInstance()));
        }
        return null;
    }

    @Override
    public void rebuild(final Iterable<ClientSubLevel> sublevels) {
        this.sectionCompiler.getBuffer().clear();
        SubLevelRenderDispatcher.super.rebuild(sublevels);
    }

    @Override
    public void updateCulling(final Iterable<ClientSubLevel> sublevels, final double cameraX, final double cameraY, final double cameraZ, final CullFrustum cullFrustum, final boolean isSpectator) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (final ClientSubLevel subLevel : sublevels) {
            final FancySubLevelRenderData renderData = (FancySubLevelRenderData) subLevel.getRenderData();
            final Pose3dc renderPose = subLevel.renderPose();
            final Vector3d plotPos = renderPose.transformPositionInverse(new Vector3d(cameraX, cameraY, cameraZ));
            final Vector3ic chunkOrigin = renderData.getChunkOrigin();

            pos.set(plotPos.x, plotPos.y, plotPos.z);
            final ClientLevel level = subLevel.getLevel();

            boolean smartCull = Minecraft.getInstance().smartCull;
            if (isSpectator && level.getBlockState(pos).isSolidRender(level, pos)) {
                smartCull = false;
            }

            renderData.getOcclusionData().update((pos.getX() >> 4) - chunkOrigin.x(), (pos.getY() >> 4) - chunkOrigin.y(), (pos.getZ() >> 4) - chunkOrigin.z(), smartCull, cullFrustum);
        }
    }

    @Override
    public void renderSectionLayer(final Iterable<ClientSubLevel> sublevels, final RenderType renderType, final ShaderInstance shader, final double cameraX, final double cameraY, final double cameraZ, final Matrix4f modelView, final Matrix4f projection, final float partialTicks) {
        final ShaderProgram program = this.getDynamicProgram(shader);
        if (program == null) {
            return;
        }

        if (!program.isValid()) {
            return;
        }

        boolean setup = false;
        final SubLevelTextureCache textureCache = this.sectionCompiler.getTextureCache();
        final ShaderUniform sableSkyLightScale = program.getUniform("SableSkyLightScale");
        final ShaderUniform sableTransform = program.getUniform("SableTransform");

        // Try to clear pending areas
        this.stagingBuffer.updateFencedAreas();
        for (final ClientSubLevel subLevel : sublevels) {
            final FancySubLevelRenderData renderData = (FancySubLevelRenderData) subLevel.getRenderData();
            final FancySubLevelOcclusionData occlusionData = renderData.getOcclusionData();

            if (!occlusionData.hasLayer(renderType)) {
                continue;
            }

            if (!setup) {
                program.bind();
                program.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, modelView, projection);
                program.bindSamplers(0);

                textureCache.bind();

                this.vertexArray.bind();
                this.sectionCompiler.getBuffer().bind(this.vertexArray);

                this.commandBuilder.setup();
                setup = true;
            }

            if (sableSkyLightScale != null) {
                final int skyLight = subLevel.getLatestSkyLightScale();
                sableSkyLightScale.setFloat(skyLight / 15.0f);
            }

            final Pose3dc renderPose = subLevel.renderPose();
            final Vector3dc renderPos = renderPose.position();
            final Quaterniondc renderRot = renderPose.orientation();
            final Vector3d renderCOR = renderRot.transform(new Vector3d(renderPose.rotationPoint()).sub(renderData.getOrigin()));

            if (sableTransform != null) {
                final Matrix4f transform = TRANSFORM.identity();

                transform.translate((float) (renderPos.x() - renderCOR.x - cameraX), (float) (renderPos.y() - renderCOR.y - cameraY), (float) (renderPos.z() - renderCOR.z - cameraZ));
                transform.rotate(new Quaternionf(renderRot));

                sableTransform.setMatrix(transform);
            }

            final Vector3d plotPos = renderPose.transformPositionInverse(new Vector3d(VeilRenderSystem.getCullingFrustum().getPosition()));
            this.commandBuilder.draw(renderData, renderType, Mth.floor(plotPos.x) >> 4, Mth.floor(plotPos.y) >> 4, Mth.floor(plotPos.z) >> 4);
        }

        // One again after to free some memory
        this.stagingBuffer.updateFencedAreas();

        if (setup) {
            this.commandBuilder.clear();
        }
    }

    @Override
    public void renderAfterSections(final Iterable<ClientSubLevel> sublevels, final double cameraX, final double cameraY, final double cameraZ, final Matrix4f modelView, final Matrix4f projection, final float partialTicks) {

    }

    @Override
    public void renderBlockEntities(final Iterable<ClientSubLevel> sublevels, final BlockEntityRenderer blockEntityRenderer, final double cameraX, final double cameraY, final double cameraZ, final float partialTick) {
        // TODO
    }

    @Override
    public void addDebugInfo(final Consumer<String> consumer) {
        consumer.accept("Staging Buffer: Used %.1f / %d MiB".formatted(this.stagingBuffer.getUsedSize() / 1024L / 1024.0, this.stagingBuffer.getSize() / 1024L / 1024L));
    }

    private void freePrograms() {
        for (final CompletableFuture<ShaderProgram> future : this.dynamicPrograms.values()) {
            future.thenAcceptAsync(ShaderProgram::free, Minecraft.getInstance());
        }
        this.dynamicPrograms.clear();
    }

    @Override
    public void free() {
        this.commandBuilder.free();
        this.sectionCompiler.free();
        this.stagingBuffer.free();
        this.vertexArray.free();
        this.freePrograms();
    }
}
