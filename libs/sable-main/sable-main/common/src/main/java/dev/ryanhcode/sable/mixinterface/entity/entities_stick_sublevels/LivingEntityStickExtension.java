package dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels;

import net.minecraft.world.phys.Vec3;

public interface LivingEntityStickExtension extends EntityStickExtension {
    void sable$setupLerp();

    void sable$applyLerp();

    Vec3 sable$getLerpTarget();
}
