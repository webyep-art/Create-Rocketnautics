package dev.ryanhcode.sable.mixin.sky_light_shadow;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // TODO: neo dies
/*
    @Inject(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V", shift = At.Shift.AFTER))
    private void sable$onRenderSectionLayer(final RenderType renderType, final double d, final double e, final double f, final Matrix4f matrix4f, final Matrix4f matrix4f2, final CallbackInfo ci, @Local final ShaderInstance shader) {
        SableSkyLightShadows.bindShadowMapTexture(shader);
    }

    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$CompiledSection;isEmpty(Lnet/minecraft/client/renderer/RenderType;)Z"))
    private boolean sable$wrapRenderSectionLayer(final SectionRenderDispatcher.CompiledSection instance, final RenderType renderType, final Operation<Boolean> original) {
        return SableSkyLightShadows.renderingShadowMap() || original.call(instance, renderType);
    }

    *//**
     * Don't render entities if we're rendering the shadow map
     *//*
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private boolean sable$wrapRenderLevel(final EntityRenderDispatcher instance, final Entity entity, final Frustum frustum, final double d, final double e, final double f, final Operation<Boolean> original) {
        return !SableSkyLightShadows.renderingShadowMap() && original.call(instance, entity, frustum, d, e, f);
    }

    @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
    private boolean sable$wrapRenderParticles(final ParticleEngine instance, final LightTexture lightTexture, final Camera camera, final float f) {
        return !SableSkyLightShadows.renderingShadowMap();
    }*/
}
