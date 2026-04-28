package dev.ryanhcode.sable.mixinterface.water_occlusion;

public interface CameraWaterOcclusionExtension {
    void sable$setIgnoreOcclusion(boolean ignore);

    boolean sable$isIgnoreOcclusion();

    boolean sable$isOccluded();
}
