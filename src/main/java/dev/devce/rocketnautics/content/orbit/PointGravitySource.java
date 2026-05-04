package dev.devce.rocketnautics.content.orbit;

import org.orekit.frames.Frame;
import org.orekit.utils.PVCoordinatesProvider;

public interface PointGravitySource extends PVCoordinatesProvider {

    /**
     * @return the attraction coefficient (m^3 / s^2) for this object.
     * When divided by distance squared, yields the acceleration towards this object.
     */
    double getMu();

    /**
     * @return The frame centered at this gravity source.
     */
    Frame getFrame();
}
