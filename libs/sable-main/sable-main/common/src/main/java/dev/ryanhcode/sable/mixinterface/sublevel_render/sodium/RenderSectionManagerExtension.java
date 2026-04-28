package dev.ryanhcode.sable.mixinterface.sublevel_render.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import org.jetbrains.annotations.Nullable;

public interface RenderSectionManagerExtension {
    @Nullable RenderSection sable$getRenderSection(int sectionX, int sectionY, int sectionZ);

    void sable$setRenderSectionDirty(int sectionX, int sectionY, int sectionZ, boolean priority);

    OcclusionCuller sable$getOcclusionCuller();

    ChunkRenderer sable$getChunkRenderer();
}
