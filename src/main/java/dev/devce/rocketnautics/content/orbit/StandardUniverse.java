package dev.devce.rocketnautics.content.orbit;

import dev.devce.rocketnautics.content.physics.SpaceTransitionHandler;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.joml.Vector3d;
import org.orekit.frames.Frame;

public class StandardUniverse extends UniverseDefinition {
    public static final StandardUniverse INSTANCE = new StandardUniverse();


    private StandardUniverse() {
        final Vector3d up = new Vector3d(0, 1, 0);
        final double solRadius = 300_000_000D;
        final double overworldRadius = 3_000_000D; // 1 / 10th of the dimension radius
        final int overworldOrbitalYearInOverworldDays = 72; // one real-life day
        final int overworldDaynightCycleLengthTicks = 24_000;
        final int overworldDaynightCycleLengthSeconds = 1200;
        final int lunarMonthInOverworldDays = 8;
        final double overworldDistance = solRadius * 40 / 3; // roughly based on the angular size of the sun in the overworld
        double earthMu = 32 * overworldRadius * overworldRadius;
        // orbit duration in seconds = 2pi * sqrt(r^3 / mu)
        // mu = r^3 * (2pi / orbit duration in seconds)^2
        // compute solar mu based on this
        double comp = overworldDistance * Math.PI / (overworldOrbitalYearInOverworldDays * overworldDaynightCycleLengthSeconds);
        double solMu = 4 * overworldDistance * comp * comp;
        // r^3 = mu * (orbit duration in seconds / 2pi)^2
        // compute moon distance based on this
        comp = lunarMonthInOverworldDays * overworldDaynightCycleLengthSeconds / Math.PI;
        // this should resolve to roughly 9 million meters away
        double moonDistance = Math.cbrt(earthMu * comp * comp / 4);
        // this should resolve to roughly 650 thousand meters
        double moonRadius = moonDistance * 3 / 40; // roughly based on the angular size of the moon in the overworld

        SimplePlanetDefinition sun = SimplePlanetDefinition.builder()
                .setFrameName("sol")
                .setMu(solMu)
                .setRadius(solRadius)
                .setRotationAxis(up)
                .setTicksPerRevolution(overworldDaynightCycleLengthTicks * 32)
                .setFixedPosition(Frame.getRoot(), Vector3D.ZERO)
                .build();
        SimplePlanetDefinition overworld = SimplePlanetDefinition.builder()
                .setFrameName("overworld")
                .setMu(earthMu)
                .setLocalSpace(SpaceTransitionHandler.SPACE_DIM)
                .setCircularOrbit(sun, Vector3D.PLUS_I.scalarMultiply(overworldDistance), Vector3D.PLUS_J)
                .setRotationAxis(up)
                .setTicksPerRevolution(overworldDaynightCycleLengthTicks)
                .build();
        SimplePlanetDefinition moon = SimplePlanetDefinition.builder()
                .setFrameName("moon")
                .setAccelerationAtSurface(8)
                .setRadius(moonRadius)
                .setCircularOrbit(overworld, Vector3D.PLUS_I.scalarMultiply(moonDistance), Vector3D.PLUS_J)
                .setTidalLocked()
                .build();
        addPlanet(sun);
        addPlanet(overworld);
        addPlanet(moon);
    }
}
