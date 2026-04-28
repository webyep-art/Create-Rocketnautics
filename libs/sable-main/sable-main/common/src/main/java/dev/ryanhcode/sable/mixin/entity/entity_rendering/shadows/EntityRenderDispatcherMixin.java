package dev.ryanhcode.sable.mixin.entity.entity_rendering.shadows;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelEntityShadowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render shadows on sub-levels
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "renderShadow", at = @At("TAIL"))
    private static void sable$renderShadowsOnSubLevels(final PoseStack poseStack,
                                                       final MultiBufferSource multiBufferSource,
                                                       final Entity entity,
                                                       final float f,
                                                       final float g,
                                                       final LevelReader levelReader,
                                                       final float shadowRadius,
                                                       final CallbackInfo ci,
                                                       @Local(ordinal = 0) final PoseStack.Pose pose,
                                                       @Local(ordinal = 0) final VertexConsumer vertexConsumer) {
        SubLevelEntityShadowRenderer.renderEntityShadowOnSubLevels(entity, f, g, shadowRadius, vertexConsumer, pose);
    }

}
