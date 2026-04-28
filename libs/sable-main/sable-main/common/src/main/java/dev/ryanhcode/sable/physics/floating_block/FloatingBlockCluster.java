package dev.ryanhcode.sable.physics.floating_block;

public class FloatingBlockCluster {
    private final FloatingBlockMaterial material;
    private final FloatingBlockData blockData;

    public FloatingBlockCluster(final FloatingBlockMaterial material) {
        this.material = material;
        this.blockData = new FloatingBlockData();
    }

    public FloatingBlockMaterial getMaterial() {
        return this.material;
    }

    public FloatingBlockData getBlockData() {
        return this.blockData;
    }
}
