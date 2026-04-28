package dev.ryanhcode.sable.mixinterface.clip_overwrite;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.Function;

public interface LevelPoseProviderExtension {
    void sable$pushPoseSupplier(Function<SubLevel, Pose3dc> supplier);

    void sable$popPoseSupplier();

    Pose3dc sable$getPose(SubLevel subLevel);
}
