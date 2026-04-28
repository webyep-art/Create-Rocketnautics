package dev.ryanhcode.sable.mixinterface.sublevel_render.sodium;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.sodium.SubLevelRenderSectionManager;
import org.jetbrains.annotations.Nullable;

public interface SodiumWorldRendererExtension {

    @Nullable SubLevelRenderSectionManager sable$getSubLevelRenderSectionManager(ClientSubLevel subLevel);

    void sable$freeRenderSectionManager(ClientSubLevel subLevel);

}
