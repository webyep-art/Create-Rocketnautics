package dev.ryanhcode.sable.sublevel.storage;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;

/**
 * The reason a sub-level was removed from a {@link SubLevelContainer}
 */
public enum SubLevelRemovalReason {
    /**
     * The sub-level was removed because it was unloaded, not clearing occupancy data
     */
    UNLOADED,

    /**
     * The sub-level was removed because it was removed from the container, clearing occupancy data
     */
    REMOVED
}
