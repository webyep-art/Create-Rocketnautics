package dev.ryanhcode.sable.mixinterface;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface BlockEntityRenderDispatcherExtension {
    void sable$setCameraPosition(@Nullable Vec3 pos);
}
