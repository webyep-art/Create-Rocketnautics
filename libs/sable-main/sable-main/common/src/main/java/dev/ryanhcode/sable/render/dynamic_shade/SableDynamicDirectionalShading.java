package dev.ryanhcode.sable.render.dynamic_shade;


public class SableDynamicDirectionalShading {

    private static boolean isEnabled = false;

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        SableDynamicDirectionalShading.isEnabled = isEnabled;
    }
}
