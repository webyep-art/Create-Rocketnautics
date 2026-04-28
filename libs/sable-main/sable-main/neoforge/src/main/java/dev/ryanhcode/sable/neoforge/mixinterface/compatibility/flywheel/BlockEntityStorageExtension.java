package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.sublevel.SubLevel;

public interface BlockEntityStorageExtension {
    void sable$setPlanVisualizationContext(VisualizationContext visualizationContext);

    SubLevelEmbedding sable$getEmbeddingInfo(SubLevel subLevel);

    void sable$preFlywheelFrame();
}
