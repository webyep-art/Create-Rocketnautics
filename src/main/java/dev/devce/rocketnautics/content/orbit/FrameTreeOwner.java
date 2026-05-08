package dev.devce.rocketnautics.content.orbit;

import org.orekit.frames.Frame;

public interface FrameTreeOwner {

    FrameTree frame();

    default int id() {
        return frame().getId();
    }

    default Frame orekitFrame() {
        return frame().getOrekitFrame();
    }
}
