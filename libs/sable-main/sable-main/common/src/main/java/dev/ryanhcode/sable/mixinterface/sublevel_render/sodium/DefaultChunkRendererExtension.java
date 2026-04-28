package dev.ryanhcode.sable.mixinterface.sublevel_render.sodium;

import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import org.jetbrains.annotations.Nullable;

public interface DefaultChunkRendererExtension {
    void sable$setCameraTransform(@Nullable CameraTransform transform);
}
