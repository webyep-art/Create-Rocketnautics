package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;

public class SubLevelNameFilter implements SubLevelSelectorModifierType.Modifier {
    private final String name;

    public SubLevelNameFilter(final String name) {
        this.name = name;
    }

    @Override
    public int getMaxResults() {
        return Integer.MAX_VALUE;
    }

    @Override
    public @Nullable List<ServerSubLevel> apply(final List<ServerSubLevel> selected, final Vector3d sourcePos) {
        final List<ServerSubLevel> filtered = new ObjectArrayList<>();
        for (final ServerSubLevel subLevel : selected) {
            if (subLevel.getName() != null && subLevel.getName().equals(this.name)) {
                filtered.add(subLevel);
            }
        }
        return filtered;
    }
}
