package dev.ryanhcode.sable.mixinterface.plot.serialization;

import net.minecraft.world.ticks.LevelChunkTicks;

public interface LevelChunkTicksExtension<T> {

    void sable$copy(final LevelChunkTicks<T> ticks);
}
