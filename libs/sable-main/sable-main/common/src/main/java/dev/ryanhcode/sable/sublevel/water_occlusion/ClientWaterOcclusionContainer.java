package dev.ryanhcode.sable.sublevel.water_occlusion;

import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegion;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * TODO: Re-do all of this state management & how we're interacting with the water occlusion renderer
 */
public class ClientWaterOcclusionContainer extends WaterOcclusionContainer<ClientWaterOcclusionContainer.ClientWaterOcclusionRegion> {
    public ClientWaterOcclusionContainer(final Level level) {
        super(level);
    }

    public static ClientWaterOcclusionContainer create(final Level level) {
        return new ClientWaterOcclusionContainer(level);
    }

    @Override
    public void removeRegion(final WaterOcclusionRegion region) {
        this.regions.remove(region);

        SableClient.WATER_OCCLUSION_RENDERER.removeRegion(((ClientWaterOcclusionRegion) region).renderRegion);
    }

    @Override
    public ClientWaterOcclusionRegion addRegion(final BoundedBitVolume3i bitSet) {
        final ClientWaterOcclusionRegion region = new ClientWaterOcclusionRegion(bitSet);
        this.regions.add(region);

        final BoundedBitVolume3i volume = region.getVolume();

        final List<BlockPos> blocks = BlockPos.betweenClosedStream(volume.getMinBlockPos(), volume.getMaxBlockPos())
                .map(BlockPos::immutable).filter(x -> volume.getOccupied(x.getX(), x.getY(), x.getZ()))
                .toList();

        region.renderRegion = SableClient.WATER_OCCLUSION_RENDERER.addRegion(blocks);

        return region;
    }

    protected static class ClientWaterOcclusionRegion extends WaterOcclusionRegion {
        private SimpleCulledRenderRegion renderRegion;

        public ClientWaterOcclusionRegion(final BoundedBitVolume3i bitSet) {
            super(bitSet);
        }
    }
}
