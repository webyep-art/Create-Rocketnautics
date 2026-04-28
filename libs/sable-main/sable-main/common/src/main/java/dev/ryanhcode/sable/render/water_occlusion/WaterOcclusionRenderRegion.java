package dev.ryanhcode.sable.render.water_occlusion;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegion;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import net.minecraft.core.BlockPos;

import java.util.Collection;

public class WaterOcclusionRenderRegion extends SimpleCulledRenderRegion {
    public WaterOcclusionRenderRegion(final Collection<BlockPos> blocks) {
        super(blocks);
    }

    @Override
    public SimpleCulledRenderRegionBuilder createMeshBuilder(final int gridSize) {
        return new SimpleCulledRenderRegionBuilder(gridSize);
    }

    @Override
    public VertexFormat getVertexFormat() {
        return DefaultVertexFormat.POSITION;
    }
}
