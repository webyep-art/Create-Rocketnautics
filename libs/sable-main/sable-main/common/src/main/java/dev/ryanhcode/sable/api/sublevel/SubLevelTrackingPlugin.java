package dev.ryanhcode.sable.api.sublevel;

import java.util.UUID;

/**
 * Other mods or projects (looking at you, Simulated!) may want to piggyback off of the snapshot interpolation
 * system so that their content can also abide by it and benefit from its improvements. As such, we expose
 * "tracking" plugins for these projects to give us players that need to be informed about the interpolation tick
 * at any given moment.
 */
public interface SubLevelTrackingPlugin {

    /**
     * Players that need to be informed about the interpolation ticks from the server &
     * the distances between them, and who should be actively running interpolation.
     */
    Iterable<UUID> neededPlayers();

    /**
     * Called when sub-level tracking data is sent
     */
    void sendTrackingData(int interpolationTick);
}
