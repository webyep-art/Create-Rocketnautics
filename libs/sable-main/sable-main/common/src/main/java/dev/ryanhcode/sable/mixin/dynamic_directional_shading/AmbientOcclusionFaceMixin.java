package dev.ryanhcode.sable.mixin.dynamic_directional_shading;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds a hook to disable the directional shading on sub-level AO block faces.
 */
@Mixin(ModelBlockRenderer.AmbientOcclusionFace.class)
public class AmbientOcclusionFaceMixin {

    @WrapOperation(method = "calculate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getShade(Lnet/minecraft/core/Direction;Z)F"))
    private float calculate(final BlockAndTintGetter instance, final Direction direction, final boolean cull, final Operation<Float> original) {
        final boolean onSubLevel = SableDynamicDirectionalShading.isEnabled() && ((ModelBlockRendererCacheExtension) ModelBlockRenderer.CACHE.get()).sable$getOnSubLevel();
        return onSubLevel ? 1.0f : original.call(instance, direction, cull);
    }
}
