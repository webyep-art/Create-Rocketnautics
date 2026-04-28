package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubLevelSortModifier implements SubLevelSelectorModifierType.Modifier {

    private final String filtering;

    public SubLevelSortModifier(final String filtering) {
        this.filtering = filtering;
    }

    @Override
    public int getMaxResults() {
        return Integer.MAX_VALUE;
    }

    @Override
    public @Nullable List<ServerSubLevel> apply(final List<ServerSubLevel> selected, final Vector3d sourcePos) {
        final Map<SubLevel, Double> distances = selected.stream().collect(Collectors.toMap(
                subLevel -> subLevel,
                subLevel -> subLevel.logicalPose().position()
                        .distanceSquared(sourcePos.x, sourcePos.y, sourcePos.z)
        ));
        if (this.filtering.equals("nearest")) {
            selected.sort(Comparator.comparingDouble(distances::get));
        } else if (this.filtering.equals("furthest")) {
            selected.sort(Comparator.comparingDouble(subLevel -> -distances.get(subLevel)));
        }

        return selected;
    }
}
