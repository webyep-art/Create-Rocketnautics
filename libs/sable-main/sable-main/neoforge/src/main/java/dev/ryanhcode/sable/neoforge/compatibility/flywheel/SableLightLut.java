package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A light look-up table based on the Flywheel / RogueLogix LUT scheme, with an extra layer for the lighting scene ID
 * First layer is studio, Y, then X, then Z.
 */
public final class SableLightLut {
    public final Layer<Layer<Layer<IntLayer>>> indices = new Layer<>();

    public void add(final int scene, final long position, final int index) {
        final var x = SectionPos.x(position);
        final var y = SectionPos.y(position);
        final var z = SectionPos.z(position);

        this.indices.computeIfAbsent(scene, Layer::new)
                .computeIfAbsent(y, Layer::new)
                .computeIfAbsent(x, IntLayer::new)
                .set(z, index + 1);
    }

    public void prune() {
        // Maybe this could be done better incrementally?
        this.indices.prune((scene) -> scene.prune((middle) -> middle.prune(IntLayer::prune)));
    }

    public void remove(final int scene, final long section) {
        final var x = SectionPos.x(section);
        final var y = SectionPos.y(section);
        final var z = SectionPos.z(section);

        final var first = this.indices.get(scene);

        if (first == null) {
            return;
        }

        final var second = first.get(y);

        if (second == null) {
            return;
        }

        final var third = second.get(x);

        if (third == null) {
            return;
        }

        third.clear(z);
    }

    public IntArrayList flatten() {
        final var out = new IntArrayList();
        this.indices.fillLut(out, (sceneIndices, lut1) -> sceneIndices.fillLut(lut1,
                (yIndices, lut2) -> yIndices.fillLut(lut2, IntLayer::fillLut)
        ));
        return out;
    }

    @FunctionalInterface
    public interface Prune<T> {
        boolean prune(T t);
    }

    public static final class Layer<T> {
        private boolean hasBase = false;
        private int base = 0;
        private Object[] nextLayer = new Object[0];

        public void fillLut(final IntArrayList lut, final BiConsumer<T, IntArrayList> inner) {
            lut.add(this.base);
            lut.add(this.nextLayer.length);

            final int innerIndexBase = lut.size();

            // Reserve space for the inner indices...
            lut.size(innerIndexBase + this.nextLayer.length);

            for (int i = 0; i < this.nextLayer.length; i++) {
                final var innerIndices = (T) this.nextLayer[i];
                if (innerIndices == null) {
                    continue;
                }

                final int layerPosition = lut.size();

                // ...so we can write in their actual positions later.
                lut.set(innerIndexBase + i, layerPosition);

                // Append the next layer to the lut.
                inner.accept(innerIndices, lut);
            }
        }

        public int base() {
            return this.base;
        }

        public int size() {
            return this.nextLayer.length;
        }

        @Nullable
        public T getRaw(final int i) {
            if (i < 0) {
                return null;
            }

            if (i >= this.nextLayer.length) {
                return null;
            }

            return (T) this.nextLayer[i];
        }

        @Nullable
        public T get(final int i) {
            if (!this.hasBase) {
                return null;
            }

            return this.getRaw(i - this.base);
        }

        public T computeIfAbsent(final int i, final Supplier<T> ifAbsent) {
            if (!this.hasBase) {
                // We don't want to default to base 0, so we'll use the first value we get.
                this.base = i;
                this.hasBase = true;
            }

            if (i < this.base) {
                this.rebase(i);
            }

            final var offset = i - this.base;

            if (offset >= this.nextLayer.length) {
                this.resize(offset + 1);
            }

            var out = this.nextLayer[offset];

            if (out == null) {
                out = ifAbsent.get();
                this.nextLayer[offset] = out;
            }
            return (T) out;
        }

        /**
         * @return {@code true} if the layer is now empty.
         */
        public boolean prune(final Prune<T> inner) {
            if (!this.hasBase) {
                return true;
            }

            // Prune the next layer before checking for leading/trailing zeros.
            for (var i = 0; i < this.nextLayer.length; i++) {
                final var o = this.nextLayer[i];
                if (o != null && inner.prune((T) o)) {
                    this.nextLayer[i] = null;
                }
            }

            final var leadingZeros = this.getLeadingZeros();

            if (leadingZeros == this.nextLayer.length) {
                return true;
            }

            final var trailingZeros = this.getTrailingZeros();

            if (leadingZeros == 0 && trailingZeros == 0) {
                return false;
            }

            final var newIndices = new Object[this.nextLayer.length - leadingZeros - trailingZeros];

            System.arraycopy(this.nextLayer, leadingZeros, newIndices, 0, newIndices.length);
            this.nextLayer = newIndices;
            this.base += leadingZeros;

            return false;
        }

        private int getLeadingZeros() {
            int out = 0;

            for (final Object index : this.nextLayer) {
                if (index == null) {
                    out++;
                } else {
                    break;
                }
            }
            return out;
        }

        private int getTrailingZeros() {
            int out = 0;

            for (int i = this.nextLayer.length - 1; i >= 0; i--) {
                if (this.nextLayer[i] == null) {
                    out++;
                } else {
                    break;
                }
            }
            return out;
        }

        private void resize(final int length) {
            final var newIndices = new Object[length];
            System.arraycopy(this.nextLayer, 0, newIndices, 0, this.nextLayer.length);
            this.nextLayer = newIndices;
        }

        private void rebase(final int newBase) {
            final var growth = this.base - newBase;

            final var newIndices = new Object[this.nextLayer.length + growth];
            // Shift the existing elements to the end of the new array to maintain their offset with the new base.
            System.arraycopy(this.nextLayer, 0, newIndices, growth, this.nextLayer.length);

            this.nextLayer = newIndices;
            this.base = newBase;
        }
    }

    public static final class IntLayer {
        private boolean hasBase = false;
        private int base = 0;
        private int[] indices = new int[0];

        public void fillLut(final IntArrayList lut) {
            lut.add(this.base);
            lut.add(this.indices.length);

            for (final int index : this.indices) {
                lut.add(index);
            }
        }

        public int base() {
            return this.base;
        }

        public int size() {
            return this.indices.length;
        }

        public int getRaw(final int i) {
            if (i < 0) {
                return 0;
            }

            if (i >= this.indices.length) {
                return 0;
            }

            return this.indices[i];
        }

        public int get(final int i) {
            if (!this.hasBase) {
                return 0;
            }

            return this.getRaw(i - this.base);
        }

        public void set(final int i, final int index) {
            if (!this.hasBase) {
                this.base = i;
                this.hasBase = true;
            }

            if (i < this.base) {
                this.rebase(i);
            }

            final var offset = i - this.base;

            if (offset >= this.indices.length) {
                this.resize(offset + 1);
            }

            this.indices[offset] = index;
        }

        /**
         * @return {@code true} if the layer is now empty.
         */
        public boolean prune() {
            if (!this.hasBase) {
                return true;
            }

            final var leadingZeros = this.getLeadingZeros();

            if (leadingZeros == this.indices.length) {
                return true;
            }

            final var trailingZeros = this.getTrailingZeros();

            if (leadingZeros == 0 && trailingZeros == 0) {
                return false;
            }

            final var newIndices = new int[this.indices.length - leadingZeros - trailingZeros];

            System.arraycopy(this.indices, leadingZeros, newIndices, 0, newIndices.length);
            this.indices = newIndices;
            this.base += leadingZeros;

            return false;
        }

        private int getTrailingZeros() {
            int out = 0;

            for (int i = this.indices.length - 1; i >= 0; i--) {
                if (this.indices[i] == 0) {
                    out++;
                } else {
                    break;
                }
            }
            return out;
        }

        private int getLeadingZeros() {
            int out = 0;

            for (final int index : this.indices) {
                if (index == 0) {
                    out++;
                } else {
                    break;
                }
            }
            return out;
        }

        public void clear(final int i) {
            if (!this.hasBase) {
                return;
            }

            if (i < this.base) {
                return;
            }

            final var offset = i - this.base;

            if (offset >= this.indices.length) {
                return;
            }

            this.indices[offset] = 0;
        }

        private void resize(final int length) {
            final var newIndices = new int[length];
            System.arraycopy(this.indices, 0, newIndices, 0, this.indices.length);
            this.indices = newIndices;
        }

        private void rebase(final int newBase) {
            final var growth = this.base - newBase;

            final var newIndices = new int[this.indices.length + growth];
            System.arraycopy(this.indices, 0, newIndices, growth, this.indices.length);

            this.indices = newIndices;
            this.base = newBase;
        }
    }
}
