package dev.ryanhcode.sable.util;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.BitSet;

/**
 * Bounded 3D bit-set, with a single bit per block in the bounds.
 */
public class BoundedBitVolume3i {
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final BitSet bitSet;

    public BoundedBitVolume3i(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            throw new IllegalArgumentException("Invalid bounding box construction");
        }

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.bitSet = new BitSet(this.volume());
    }

    @Nullable
    public static BoundedBitVolume3i fromBlocks(final Iterable<BlockPos> blocks) {
        Vector3i minBlockPos = null;
        Vector3i maxBlockPos = null;

        for (final BlockPos block : blocks) {
            if (minBlockPos == null) {
                minBlockPos = new Vector3i().set(block.getX(), block.getY(), block.getZ());
                maxBlockPos = new Vector3i().set(block.getX(), block.getY(), block.getZ());
            }

            final Vector3i blockVector3i = new Vector3i(block.getX(), block.getY(), block.getZ());
            minBlockPos.min(blockVector3i);
            maxBlockPos.max(blockVector3i);
        }

        if (minBlockPos == null) {
            return null;
        }

        final BoundedBitVolume3i set = new BoundedBitVolume3i(minBlockPos.x, minBlockPos.y, minBlockPos.z, maxBlockPos.x, maxBlockPos.y, maxBlockPos.z);

        for (final BlockPos block : blocks) {
            set.setOccupied(block.getX(), block.getY(), block.getZ(), true);
        }

        return set;
    }

    /**
     * Sets the occupied status for a global cell
     */
    public void setOccupied(final int x, final int y, final int z, final boolean occupied) {
        if (!this.isInBounds(x, y, z)) {
            throw new IllegalArgumentException("Cannot set out of bounds!");
        }

        this.bitSet.set(this.getIndex(x, y, z), occupied);
    }

    /**
     * Gets the occupied status for a global cell
     *
     * @return the occupied status for the cell, or false if out of bounds
     */
    public boolean getOccupied(final int x, final int y, final int z) {
        if (!this.isInBounds(x, y, z)) {
            return false;
        }

        return this.bitSet.get(this.getIndex(x, y, z));
    }

    /**
     * @return the index for a global coordinate, or -1 if out of bounds
     */
    public int getIndex(final int x, final int y, final int z) {
        if (!this.isInBounds(x, y, z)) {
            return -1;
        }

        final int localX = x - this.minX;
        final int localY = y - this.minY;
        final int localZ = z - this.minZ;

        return (localX * this.zSpan() * this.ySpan()) + (localZ * this.ySpan()) + localY;
    }

    public int volume() {
        return this.xSpan() * this.ySpan() * this.zSpan();
    }

    public int xSpan() {
        return this.maxX - this.minX + 1;
    }

    public int ySpan() {
        return this.maxY - this.minY + 1;
    }

    public int zSpan() {
        return this.maxZ - this.minZ + 1;
    }

    public boolean isInBounds(final int x, final int y, final int z) {
        return x >= this.minX && x <= this.maxX &&
                y >= this.minY && y <= this.maxY &&
                z >= this.minZ && z <= this.maxZ;
    }

    public BlockPos getMinBlockPos() {
        return new BlockPos(this.minX, this.minY, this.minZ);
    }

    public BlockPos getMaxBlockPos() {
        return new BlockPos(this.maxX, this.maxY, this.maxZ);
    }
}
