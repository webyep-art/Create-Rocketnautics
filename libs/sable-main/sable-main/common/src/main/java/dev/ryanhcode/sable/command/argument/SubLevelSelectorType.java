package dev.ryanhcode.sable.command.argument;

import net.minecraft.network.chat.Component;

public enum SubLevelSelectorType {
    ALL('e', Component.translatable("argument.sable.body.selector.all"), false),
    NEAREST('n', Component.translatable("argument.sable.body.selector.nearest"), true),
    RANDOM('r', Component.translatable("argument.sable.body.selector.random"), true),
    VIEWED('v', Component.translatable("argument.sable.body.selector.viewed"), true),
    LATEST('l', Component.translatable("argument.sable.body.selector.latest"), true),
    TRACKING('t', Component.translatable("argument.sable.body.selector.tracking"), true),
    INSIDE('i', Component.translatable("argument.sable.body.selector.inside"), true);

    private final char selector;
    private final Component tooltip;
    private final boolean single;

    SubLevelSelectorType(final char selector, final Component tooltip, final boolean single) {
        this.selector = selector;
        this.tooltip = tooltip;
        this.single = single;
    }

    public static SubLevelSelectorType of(final char c) {
        for (final SubLevelSelectorType type : SubLevelSelectorType.values()) {
            if (type.selector == c) {
                return type;
            }
        }

        return null;
    }

    public char getChar() {
        return this.selector;
    }

    public Component getTooltip() {
        return this.tooltip;
    }

    public boolean single() {
        return this.single;
    }
}
