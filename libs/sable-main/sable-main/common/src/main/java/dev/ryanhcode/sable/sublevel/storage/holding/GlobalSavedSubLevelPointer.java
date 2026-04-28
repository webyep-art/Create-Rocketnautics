package dev.ryanhcode.sable.sublevel.storage.holding;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;

/**
 * A global location of sub-level storage
 */
public record GlobalSavedSubLevelPointer(ChunkPos chunkPos, short storageIndex, short subLevelIndex) {

    public static final Codec<GlobalSavedSubLevelPointer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("chunk_x").forGetter(x -> x.chunkPos().x),
            Codec.INT.fieldOf("chunk_z").forGetter(x -> x.chunkPos().z),
            Codec.SHORT.fieldOf("storage_index").forGetter(GlobalSavedSubLevelPointer::storageIndex),
            Codec.SHORT.fieldOf("sub_level_index").forGetter(GlobalSavedSubLevelPointer::subLevelIndex)
    ).apply(Applicative.unbox(instance), (chunkX, chunkZ, storage, subLevel) -> new GlobalSavedSubLevelPointer(new ChunkPos(chunkX, chunkZ), storage, subLevel)));

    public SavedSubLevelPointer local() {
        return new SavedSubLevelPointer(this.storageIndex, this.subLevelIndex);
    }

    @Override
    public String toString() {
        return "global->[" +
                "chunkPos=" + this.chunkPos +
                ", storageIndex=" + this.storageIndex +
                ", subLevelIndex=" + this.subLevelIndex +
                ']';
    }
}
