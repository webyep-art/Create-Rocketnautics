package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.critereon.MinMaxBounds;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class SubLevelDoubleRangeFilter implements SubLevelSelectorModifierType.Modifier {
    private final MinMaxBounds.Doubles range;
    private final DoubleGetter valueGetter;
    private final boolean squared;

    private SubLevelDoubleRangeFilter(final MinMaxBounds.Doubles range, final DoubleGetter valueGetter, final boolean squared) {
        this.range = range;
        this.valueGetter = valueGetter;
        this.squared = squared;
    }

    public static SubLevelDoubleRangeFilter.Factory linear(final DoubleGetter valueGetter) {
        return new SubLevelDoubleRangeFilter.Factory(valueGetter, false);
    }

    public static SubLevelDoubleRangeFilter.Factory squared(final DoubleGetter valueGetter) {
        return new SubLevelDoubleRangeFilter.Factory(valueGetter, true);
    }

    @Override
    public int getMaxResults() {
        return Integer.MAX_VALUE;
    }

    @Override
    public @Nullable List<ServerSubLevel> apply(final List<ServerSubLevel> selected, final Vector3d sourcePos) {
        final List<ServerSubLevel> filtered = new ObjectArrayList<>();

        for (final ServerSubLevel subLevel : selected) {
            final double value = this.valueGetter.fromSublevel(subLevel, sourcePos);
            if (this.squared) {
                if (this.range.matchesSqr(value)) {
                    filtered.add(subLevel);
                }
            } else {
                if (this.range.matches(value)) {
                    filtered.add(subLevel);
                }
            }
        }

        return filtered;
    }

    @FunctionalInterface
    public interface DoubleGetter {
        double fromSublevel(ServerSubLevel subLevel, Vector3dc sourcePos);
    }

    public static class Factory {
        private final DoubleGetter doubleGetter;
        private final boolean squared;

        public Factory(final DoubleGetter doubleGetter, final boolean squared) {
            this.doubleGetter = doubleGetter;
            this.squared = squared;
        }

        public SubLevelDoubleRangeFilter create(final MinMaxBounds.Doubles range) {
            return new SubLevelDoubleRangeFilter(range, this.doubleGetter, this.squared);
        }
    }
}
