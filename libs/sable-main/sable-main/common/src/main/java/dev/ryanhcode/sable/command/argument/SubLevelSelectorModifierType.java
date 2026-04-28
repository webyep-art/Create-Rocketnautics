package dev.ryanhcode.sable.command.argument;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubLevelSelectorModifierType {

    private static final Map<String, SubLevelSelectorModifierType> MODIFIERS_BY_NAME = new Object2ObjectOpenHashMap<>();

    private static final SimpleCommandExceptionType UNKNOWN_PROPERTY_NAME =
            new SimpleCommandExceptionType(Component.translatable("argument.sable.sub_level.unknown_property"));

    private final String name;
    private final Parser parser;
    private final FilterPriority filterPriority;

    public SubLevelSelectorModifierType(final String name, final Parser parser, final FilterPriority priority) {
        this.name = name;
        this.parser = parser;
        this.filterPriority = priority;
    }

    public static void registerType(final String name, final Parser parser, final FilterPriority filterPriority) {
        if (MODIFIERS_BY_NAME.containsKey(name)) {
            throw new IllegalArgumentException("Modifier type " + name + " already registered");
        }
        MODIFIERS_BY_NAME.put(name, new SubLevelSelectorModifierType(name, parser, filterPriority));
    }

    public static SubLevelSelectorModifierType getModifier(final String propertyName, final StringReader readerForErrorContext) throws CommandSyntaxException {
        if (!MODIFIERS_BY_NAME.containsKey(propertyName)) {
            throw UNKNOWN_PROPERTY_NAME.createWithContext(readerForErrorContext);
        }
        return MODIFIERS_BY_NAME.get(propertyName);
    }

    public static void clearRegistry() {
        MODIFIERS_BY_NAME.clear();
    }

    public static List<Pair<String, Message>> getAllNamesWithTooltip() {
        final ArrayList<Pair<String, Message>> modifiers = new ArrayList<>();
        for (final SubLevelSelectorModifierType modifier : MODIFIERS_BY_NAME.values()) {
            modifiers.add(Pair.of(modifier.name, Component.translatable("argument.sable.sub_level.modifier." + modifier.name)));
        }
        return modifiers;
    }

    public Parser getParser() {
        return this.parser;
    }

    public FilterPriority getFilterPriority() {
        return this.filterPriority;
    }

    /**
     * Ensures that something like {@code [limit=1,sort=nearest]} applies the sort first, then the limit
     */
    public enum FilterPriority {
        POSITION,
        FILTER,
        SORTING,
        SORTING_SELECTION,
    }

    public interface Parser {
        SubLevelSelectorModifierType.Modifier parse(final StringReader value) throws CommandSyntaxException;
    }

    public interface Modifier {
        /**
         * @return the maximum quantity of sub-levels this modifier could ever produce
         */
        int getMaxResults();

        /**
         * Applies the modifier to the selected sub-levels
         *
         * @param selected  The currently selected sub-levels
         * @param sourcePos The position of the source, should only be modified by modifiers with priority of {@link FilterPriority#POSITION}
         * @return The filtered sub-levels
         */
        @Nullable List<ServerSubLevel> apply(final List<ServerSubLevel> selected, Vector3d sourcePos);
    }

}
