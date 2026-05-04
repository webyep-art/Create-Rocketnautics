package dev.devce.rocketnautics.content.orbit;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.orekit.frames.Frame;

import java.util.*;

public class UniverseDefinition {
    private final Map<String, Frame> frames = new Object2ObjectOpenHashMap<>();
    private final Set<PointGravitySource> gravitySources = new HashSet<>();

    protected void addPlanet(CubePlanetDefinition planet) {
        addFrame(planet.getFrame());
        gravitySources.add(planet);
    }

    protected void addFrame(Frame frame) {
        frames.put(frame.getName(), frame);
    }

    @UnmodifiableView
    public Collection<PointGravitySource> getGravitySources() {
        return gravitySources;
    }

    @Nullable
    public Frame getFrameByName(String name) {
        if ("root".equals(name) || "".equals(name) || Frame.getRoot().getName().equals(name)) {
            return Frame.getRoot();
        }
        return frames.get(name);
    }
}
