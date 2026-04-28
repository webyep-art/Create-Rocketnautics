package dev.ryanhcode.sable.mixinhelpers.voxel_shape_iteration;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.BitSet;

/**
 * This manually replicates the behavior from {@link BitSet} with all checks removed.
 * A second internal array exists for resetting to the original state.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class LongArrayDiscreteVoxelShape extends DiscreteVoxelShape {

    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final long WORD_MASK = 0xffffffffffffffffL;

    private final long[] baseWords;
    private final long[] words;

    public LongArrayDiscreteVoxelShape(final DiscreteVoxelShape shape, final int xSize, final int ySize, final int zSize) {
        super(xSize, ySize, zSize);
        this.words = new long[wordIndex(xSize * ySize * zSize - 1) + 1];
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    if (shape.isFull(x, y, z)) {
                        final int bitIndex = this.getIndex(x, y, z);
                        this.words[wordIndex(bitIndex)] |= (1L << bitIndex);
                    }
                }
            }
        }
        this.baseWords = Arrays.copyOf(this.words, this.words.length);
    }

    private static int wordIndex(final int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    private int getIndex(final int x, final int y, final int z) {
        return (x * this.ySize + y) * this.zSize + z;
    }

    private int nextClearBit(final int fromIndex) {
        int u = wordIndex(fromIndex);
        long word = ~words[u] & (WORD_MASK << fromIndex);
        while (true) {
            if (word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == this.words.length) {
                return this.words.length * BITS_PER_WORD;
            }
            word = ~words[u];
        }
    }

    private void clear(final int fromIndex, int toIndex) {
        final int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        if (endWordIndex >= this.words.length) {
            toIndex = BITS_PER_WORD * (this.words.length - 1) + (BITS_PER_WORD - Long.numberOfLeadingZeros(words[this.words.length - 1]));
            endWordIndex = this.words.length - 1;
        }

        final long firstWordMask = WORD_MASK << fromIndex;
        final long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            this.words[startWordIndex] &= ~(firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            this.words[startWordIndex] &= ~firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                this.words[i] = 0;
            }

            // Handle last word
            this.words[endWordIndex] &= ~lastWordMask;
        }
    }

    public void reset() {
        System.arraycopy(this.baseWords, 0, this.words, 0, this.words.length);
    }

    public boolean isZStripFull(final int i, final int j, final int k, final int l) {
        return k < this.xSize && l < this.ySize && this.nextClearBit(this.getIndex(k, l, i)) >= this.getIndex(k, l, j);
    }

    public boolean isXZRectangleFull(final int i, final int j, final int k, final int l, final int m) {
        for (int n = i; n < j; n++) {
            if (!this.isZStripFull(k, l, n, m)) {
                return false;
            }
        }

        return true;
    }

    public void clearZStrip(final int i, final int j, final int k, final int l) {
        this.clear(this.getIndex(k, l, i), this.getIndex(k, l, j));
    }

    @Override
    public boolean isFull(final int x, final int y, final int z) {
        final int bitIndex = this.getIndex(x, y, z);
        final int wordIndex = wordIndex(bitIndex);
        return (wordIndex < this.words.length) && (this.words[wordIndex] & (1L << bitIndex)) != 0;
    }

    @Override
    public void fill(final int i, final int j, final int k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int firstFull(final Direction.Axis axis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastFull(final Direction.Axis axis) {
        throw new UnsupportedOperationException();
    }
}
