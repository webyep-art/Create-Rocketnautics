package dev.ryanhcode.sable.sublevel.water_occlusion;

import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import net.minecraft.world.level.Level;

public class ServerWaterOcclusionContainer extends WaterOcclusionContainer<WaterOcclusionRegion> {

    public static ServerWaterOcclusionContainer create(final Level level) {
        return new ServerWaterOcclusionContainer(level);
    }

    public ServerWaterOcclusionContainer(final Level level) {
        super(level);
    }

    @Override
    public void removeRegion(final WaterOcclusionRegion region) {
        this.regions.remove(region);
    }

    @Override
    public WaterOcclusionRegion addRegion(final BoundedBitVolume3i bitSet) {
        final WaterOcclusionRegion region = new WaterOcclusionRegion(bitSet);
        this.regions.add(region);
        return region;
    }
}
