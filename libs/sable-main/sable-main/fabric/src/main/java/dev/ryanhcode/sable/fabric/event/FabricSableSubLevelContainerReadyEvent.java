package dev.ryanhcode.sable.fabric.event;

import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when Sable has finished initialization for a level and its sub-level container is ready to use.
 */
@FunctionalInterface
public interface FabricSableSubLevelContainerReadyEvent extends SableSubLevelContainerReadyEvent {
    Event<SableSubLevelContainerReadyEvent> EVENT = EventFactory.createArrayBacked(SableSubLevelContainerReadyEvent.class, (events) -> (level, container) -> {
        for (final SableSubLevelContainerReadyEvent event : events) {
            event.onSubLevelContainerReady(level, container);
        }
    });
}
