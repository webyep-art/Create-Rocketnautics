package dev.ryanhcode.sable.sublevel.render.fancy;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelTextureCache;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3ic;
import org.lwjgl.system.NativeResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubLevelMeshBuilder {

    private static final Direction[] DIRECTIONS = Direction.values();

    private final BlockRenderDispatcher blockRenderer;
    private final BlockEntityRenderDispatcher blockEntityRenderer;
    private final SubLevelTextureCache textureCache;

    public SubLevelMeshBuilder(
            final BlockRenderDispatcher blockRenderDispatcher,
            final BlockEntityRenderDispatcher blockEntityRenderDispatcher,
            final SubLevelTextureCache textureCache) {
        this.blockRenderer = blockRenderDispatcher;
        this.blockEntityRenderer = blockEntityRenderDispatcher;
        this.textureCache = textureCache;
    }

    public Results compile(final Vector3ic origin, final SectionPos sectionPos, final RenderChunkRegion renderChunkRegion, final SectionBufferBuilderPack sectionBufferBuilderPack) {
        final Results results = new Results();
        final BlockPos min = sectionPos.origin();
        final BlockPos max = min.offset(15, 15, 15);
        final VisGraph visGraph = new VisGraph();
        final PoseStack poseStack = new PoseStack();
        ModelBlockRenderer.enableCaching();
        final Map<RenderType, QuadMesh> faceMeshes = new Reference2ObjectArrayMap<>(RenderType.chunkBufferLayers().size());
        final RandomSource randomSource = RandomSource.create();
        final BlockPos.MutableBlockPos offsetPos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos rightPos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos upPos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos forwardPos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos aoOffsetPos = new BlockPos.MutableBlockPos();

        for (final BlockPos pos : BlockPos.betweenClosed(min, max)) {
            final BlockState blockState = renderChunkRegion.getBlockState(pos);
            if (blockState.isSolidRender(renderChunkRegion, pos)) {
                visGraph.setOpaque(pos);
            }

            if (blockState.hasBlockEntity()) {
                final BlockEntity blockEntity = renderChunkRegion.getBlockEntity(pos);
                if (blockEntity != null) {
                    this.handleBlockEntity(results, blockEntity);
                }
            }

            // TODO
//            final FluidState fluidState = blockState.getFluidState();
//            if (!fluidState.isEmpty()) {
//                final RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
//                final BufferBuilder bufferBuilder = this.getOrBeginLayer(map, sectionBufferBuilderPack, renderType);
//                this.blockRenderer.renderLiquid(blockPos3, renderChunkRegion, bufferBuilder, blockState, fluidState);
//            }

            if (blockState.getRenderShape() == RenderShape.MODEL) {
                final RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(blockState);

                final BakedModel model = this.blockRenderer.getBlockModel(blockState);
                final long seed = blockState.getSeed(pos);

                randomSource.setSeed(seed);
                final List<BakedQuad> unculledQuads = model.getQuads(blockState, null, randomSource);
                if (unculledQuads.isEmpty()) {
                    final BakedQuad[] quads = new BakedQuad[6];
                    boolean valid = true;

                    for (final Direction direction : DIRECTIONS) {
                        randomSource.setSeed(seed);
                        final List<BakedQuad> culledQuads = model.getQuads(blockState, direction, randomSource);
                        if (culledQuads.size() != 1) {
                            valid = false;
                            break;
                        }

                        final BakedQuad quad = culledQuads.getFirst();
                        if (!isAxisAligned(quad)) {
                            valid = false;
                            break;
                        }

                        quads[direction.get3DDataValue()] = quad;
                    }

                    if (valid) {
                        final QuadMesh mesh = faceMeshes.computeIfAbsent(renderType, unused -> new QuadMesh());

                        final int posX = pos.getX() & 15;
                        final int posY = pos.getY() & 15;
                        final int posZ = pos.getZ() & 15;

                        for (final Direction direction : DIRECTIONS) {
                            offsetPos.setWithOffset(pos, direction);
                            if (Block.shouldRenderFace(blockState, renderChunkRegion, pos, direction, offsetPos)) {
                                final int packedLight = LevelRenderer.getLightColor(renderChunkRegion, blockState, offsetPos);
                                final int blockLight = LightTexture.block(packedLight);
                                final int skyLight = LightTexture.sky(packedLight);
                                final int textureId = this.textureCache.getTextureId(quads[direction.get3DDataValue()]);
                                final int packedData = posX | posY << 4 | posZ << 8 | skyLight << 12 | blockLight << 16 | textureId << 20;
                                final IntList face = mesh.faces[direction.get3DDataValue()];
                                face.add(packedData);
                                face.add((sectionPos.x() - origin.x()) | ((sectionPos.y() - origin.y()) << 8) | ((sectionPos.z() - origin.z()) << 16) | getFaceAO(renderChunkRegion, offsetPos, direction, aoOffsetPos, rightPos, upPos, forwardPos));
                            }
                        }
                        continue;
                    }
                }

                System.out.printf("Block at %s isn't a cube %n", pos);
                // TODO
//                poseStack.pushPose();
//                poseStack.translate((float) SectionPos.sectionRelative(pos.getX()), (float) SectionPos.sectionRelative(pos.getY()), (float) SectionPos.sectionRelative(pos.getZ()));
//                this.blockRenderer.renderBatched(blockState, pos, renderChunkRegion, poseStack, bufferBuilder, true, randomSource);
//                poseStack.popPose();
            }
        }

        results.renderedQuadLayers.putAll(faceMeshes);

        ModelBlockRenderer.clearCache();
        results.visibilitySet = visGraph.resolve();
        return results;
    }

    private static int getFaceAO(final BlockAndTintGetter level, final BlockPos pos, final Direction direction, final BlockPos.MutableBlockPos offset, final BlockPos.MutableBlockPos right, final BlockPos.MutableBlockPos up, final BlockPos.MutableBlockPos forward) {
        if (!Minecraft.useAmbientOcclusion()) {
            return 0;
        }

        switch (direction) {
            case DOWN -> {
                right.set(0, 0, -1);
                up.set(1, 0, 0);
            }
            case UP -> {
                right.set(0, 0, 1);
                up.set(1, 0, 0);
            }
            case NORTH -> {
                right.set(-1, 0, 0);
                up.set(0, 1, 0);
            }
            case SOUTH -> {
                right.set(1, 0, 0);
                up.set(0, 1, 0);
            }
            case WEST -> {
                right.set(0, 0, 1);
                up.set(0, 1, 0);
            }
            case EAST -> {
                right.set(0, 0, -1);
                up.set(0, 1, 0);
            }
        }

        offset.setWithOffset(pos, -up.getX(), -up.getY(), -up.getZ());
        final boolean downAO = isOpaque(level, offset);
        final boolean downLeftAO = isOpaque(level, offset.move(-right.getX(), -right.getY(), -right.getZ()));
        final boolean leftAO = isOpaque(level, offset.move(up.getX(), up.getY(), up.getZ()));
        final boolean upLeftAO = isOpaque(level, offset.move(up.getX(), up.getY(), up.getZ()));
        final boolean upAO = isOpaque(level, offset.move(right.getX(), right.getY(), right.getZ()));
        final boolean upRightAO = isOpaque(level, offset.move(right.getX(), right.getY(), right.getZ()));
        final boolean rightAO = isOpaque(level, offset.move(-up.getX(), -up.getY(), -up.getZ()));
        final boolean downRightAO = isOpaque(level, offset.move(-up.getX(), -up.getY(), -up.getZ()));

        final int ao0 = vertexAO(downAO, leftAO, downLeftAO);
        final int ao1 = vertexAO(downAO, rightAO, downRightAO);
        final int ao2 = vertexAO(upAO, rightAO, upRightAO);
        final int ao3 = vertexAO(upAO, leftAO, upLeftAO);

        return switch (direction) {
            case NORTH, SOUTH, WEST, EAST -> ao3 << 24 | ao0 << 26 | ao1 << 28 | ao2 << 30;
            default -> ao0 << 24 | ao1 << 26 | ao2 << 28 | ao3 << 30;
        };
    }

    private static int vertexAO(final boolean side1, final boolean side2, final boolean corner) {
        if (side1 && side2) {
            return 3;
        }

        return (side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0);
    }

    private static boolean isOpaque(final BlockAndTintGetter level, final BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private static boolean isAxisAligned(final BakedQuad quad) {
        final int[] vertices = quad.getVertices();
        for (int i = 0; i < vertices.length / 8; i++) {
            final float x = Float.intBitsToFloat(vertices[i * 8]);
            final float y = Float.intBitsToFloat(vertices[i * 8 + 1]);
            final float z = Float.intBitsToFloat(vertices[i * 8 + 2]);
            if (Math.abs(x - Math.round(x)) > 1e-2 || Math.abs(y - Math.round(y)) > 1e-2 || Math.abs(z - Math.round(z)) > 1e-2) {
                return false;
            }
        }
        return true;
    }

    private ByteBufferBuilder getOrBeginQuadLayer(final Map<RenderType, ByteBufferBuilder> map, final SectionBufferBuilderPack pack, final RenderType renderType) {
        ByteBufferBuilder bufferBuilder = map.get(renderType);
        if (bufferBuilder == null) {
            bufferBuilder = pack.buffer(renderType);
            map.put(renderType, bufferBuilder);
        }

        return bufferBuilder;
    }

    private <E extends BlockEntity> void handleBlockEntity(final Results results, final E blockEntity) {
        final BlockEntityRenderer<E> blockEntityRenderer = this.blockEntityRenderer.getRenderer(blockEntity);
        if (blockEntityRenderer != null) {
            if (blockEntityRenderer.shouldRenderOffScreen(blockEntity)) {
                results.globalBlockEntities.add(blockEntity);
            } else {
                results.blockEntities.add(blockEntity);
            }
        }
    }

    public static class QuadMesh {

        private final IntList[] faces;

        public QuadMesh() {
            this.faces = new IntArrayList[DIRECTIONS.length];
            for (int i = 0; i < this.faces.length; i++) {
                this.faces[i] = new IntArrayList();
            }
        }

        public IntList[] getFaces() {
            return this.faces;
        }
    }

    public static final class Results implements NativeResource {
        public final List<BlockEntity> globalBlockEntities = new ArrayList<>();
        public final List<BlockEntity> blockEntities = new ArrayList<>();
        public final Map<RenderType, QuadMesh> renderedQuadLayers = new Reference2ObjectArrayMap<>();
        public final Map<RenderType, MeshData> renderedModelLayers = new Reference2ObjectArrayMap<>();
        public VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        public MeshData.SortState transparencyState;

        @Override
        public void free() {
            this.renderedModelLayers.values().forEach(MeshData::close);
        }
    }
}
