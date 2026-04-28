package dev.ryanhcode.sable.mixin.water_occlusion;

import dev.ryanhcode.sable.mixinterface.water_occlusion.CameraWaterOcclusionExtension;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin implements CameraWaterOcclusionExtension {

    @Shadow
    private Vec3 position;

    @Shadow
    private BlockGetter level;

    @Unique
    private boolean sable$ignoreOcclusion = false;

    @Inject(method = "getFluidInCamera", at = @At("RETURN"), cancellable = true)
    public void sable$getFluidInCamera(final CallbackInfoReturnable<FogType> cir) {
        if (this.sable$ignoreOcclusion) {
            return;
        }

        if (cir.getReturnValue() == FogType.WATER || cir.getReturnValue() == FogType.LAVA) {
            final boolean occluded = this.sable$isOccluded();

            if (occluded) {
                cir.setReturnValue(FogType.NONE);
            }
        }
    }

    @Override
    public void sable$setIgnoreOcclusion(final boolean ignore) {
        this.sable$ignoreOcclusion = ignore;
    }

    @Override
    public boolean sable$isIgnoreOcclusion() {
        return this.sable$ignoreOcclusion;
    }

    @Override
    public boolean sable$isOccluded() {
        final WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer((Level) this.level);

        if (container == null)
            return false;

        return container.isOccluded(this.position);
    }
}
