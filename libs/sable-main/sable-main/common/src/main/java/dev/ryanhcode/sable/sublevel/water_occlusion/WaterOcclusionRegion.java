package dev.ryanhcode.sable.sublevel.water_occlusion;

import dev.ryanhcode.sable.util.BoundedBitVolume3i;

public class WaterOcclusionRegion {
    private final BoundedBitVolume3i bitSet;
    private boolean dirty = false;

    public WaterOcclusionRegion(final BoundedBitVolume3i bitSet) {
        this.bitSet = bitSet;
    }

    public BoundedBitVolume3i getVolume() {
        return this.bitSet;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }
}
