package dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface EntityStickExtension {

    void sable$plotLerpTo(Vec3 pos, int lerpSteps);

    void sable$setPlotPosition(@Nullable Vec3 position);

    @Nullable Vec3 sable$getPlotPosition();

}
