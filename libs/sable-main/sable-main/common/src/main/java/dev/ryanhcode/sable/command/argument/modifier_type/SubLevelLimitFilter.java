package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;

public class SubLevelLimitFilter implements SubLevelSelectorModifierType.Modifier {
    private final int limit;

    public SubLevelLimitFilter(final int limit) {
        this.limit = limit;
    }

    @Override
    public int getMaxResults() {
        return this.limit;
    }

    @Override
    public @Nullable List<ServerSubLevel> apply(final List<ServerSubLevel> selected, final Vector3d sourcePos) {
        if (selected.size() > this.limit) {
            return new ObjectArrayList<>(selected.subList(0, this.limit));
        }
        return selected;
    }
}
