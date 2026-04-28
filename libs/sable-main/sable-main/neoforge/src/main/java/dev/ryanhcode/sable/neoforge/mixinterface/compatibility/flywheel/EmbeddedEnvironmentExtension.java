package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.flywheel;

import org.joml.Matrix4fc;

public interface EmbeddedEnvironmentExtension {
    void sable$setLightingInfo(Matrix4fc sceneMatrix, int scene, float skyLightScale);
}
