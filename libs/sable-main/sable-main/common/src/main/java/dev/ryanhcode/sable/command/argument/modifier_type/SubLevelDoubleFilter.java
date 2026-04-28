package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class SubLevelDoubleFilter implements SubLevelSelectorModifierType.Modifier {
    private final double value;
    private final DoublePredicate valuePredicate;

    private SubLevelDoubleFilter(final double value, final DoublePredicate valuePredicate) {
        this.value = value;
        this.valuePredicate = valuePredicate;
    }

    public static SubLevelDoubleFilter.Factory factory(final DoublePredicate valuePredicate) {
        return new SubLevelDoubleFilter.Factory(valuePredicate);
    }

    @Override
    public int getMaxResults() {
        return Integer.MAX_VALUE;
    }

    @Override
    public @Nullable List<ServerSubLevel> apply(final List<ServerSubLevel> selected, final Vector3d sourcePos) {
        final List<ServerSubLevel> filtered = new ObjectArrayList<>();

        for (final ServerSubLevel subLevel : selected) {
            if (this.valuePredicate.fromSublevel(subLevel, sourcePos, this.value)) {
                filtered.add(subLevel);
            }
        }

        return filtered;
    }

    @FunctionalInterface
    public interface DoublePredicate {
        boolean fromSublevel(ServerSubLevel subLevel, Vector3dc sourcePos, double test);
    }

    public static class Factory {
        private final DoublePredicate doublePredicate;

        public Factory(final DoublePredicate doublePredicate) {
            this.doublePredicate = doublePredicate;
        }

        public SubLevelDoubleFilter create(final double value) {
            return new SubLevelDoubleFilter(value, this.doublePredicate);
        }
    }
}
