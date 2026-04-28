package dev.ryanhcode.sable.mixinterface.sublevel_render.sodium;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import org.jetbrains.annotations.Nullable;

public interface OcclusionCullerExtension {
    void sable$setSubLevel(@Nullable ClientSubLevel subLevel);
}
