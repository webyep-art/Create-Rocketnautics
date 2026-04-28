package dev.ryanhcode.sable.sublevel.plot;

/**
 * A chunk section that contains auxiliary data for managing sub-level splitting
 */
public class HeatDataChunkSection {

    /**
     * The number of heatmap data points
     */
    public static final int SIZE = 16 * 16 * 16;

    /**
     * The data points
     */
    private final short[] data = new short[SIZE];

    /**
     * Gets the index of a data point at a position local to the section
     */
    public static int getIndex(final int x, final int y, final int z) {
        return (y << 8) | (z << 4) | x;
    }

    /**
     * Gets the data point at a position local to the section
     */
    public short get(final int x, final int y, final int z) {
        return this.data[getIndex(x, y, z)];
    }

    /**
     * Sets the data point at a position local to the section
     */
    public void set(final int x, final int y, final int z, final short value) {
        this.data[getIndex(x, y, z)] = value;
    }
}
