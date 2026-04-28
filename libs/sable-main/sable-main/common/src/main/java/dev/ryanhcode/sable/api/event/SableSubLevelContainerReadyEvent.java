package dev.ryanhcode.sable.api.event;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.world.level.Level;

/**
 * Fired when Sable has finished initialization for a level and its sub-level container is ready to use.
 */
@FunctionalInterface
public interface SableSubLevelContainerReadyEvent {

    /**
     * Called when a sub-level container is ready to use.
     *
     * @param level The level instance
     * @param container The sub-level container that is ready
     */
    void onSubLevelContainerReady(Level level, SubLevelContainer container);

}
