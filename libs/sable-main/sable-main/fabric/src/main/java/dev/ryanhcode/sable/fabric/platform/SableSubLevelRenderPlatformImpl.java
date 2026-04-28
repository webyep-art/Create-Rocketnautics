package dev.ryanhcode.sable.fabric.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.platform.SableSubLevelRenderPlatform;
import dev.ryanhcode.sable.sublevel.render.vanilla.SingleBlockSubLevelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@ApiStatus.Internal
public class SableSubLevelRenderPlatformImpl implements SableSubLevelRenderPlatform {

    @Override
    public void tesselateBlock(final SingleBlockSubLevelWrapper blockAndTintGetter, final BakedModel bakedModel, final BlockState blockState, final BlockPos pos, final PoseStack poseStack, final VertexConsumer vertexConsumer, final RandomSource randomSource, final long seed, final int packedOverlay, @Nullable final RenderType renderType) {
        Minecraft.getInstance().getBlockRenderer().modelRenderer.tesselateWithoutAO(blockAndTintGetter, bakedModel, blockState, pos, poseStack, vertexConsumer, true, randomSource, seed, packedOverlay);
    }

    @Override
    public List<RenderType> getRenderLayers(final SingleBlockSubLevelWrapper blockAndTintGetter, final BakedModel bakedModel, final BlockState blockState, final BlockPos pos, final RandomSource randomSource) {
        return List.of(ItemBlockRenderTypes.getChunkRenderType(blockState));
    }

    @Override
    public void tryAddFlywheelVisual(final BlockEntity blockEntity) {
        // no-op
    }
}
