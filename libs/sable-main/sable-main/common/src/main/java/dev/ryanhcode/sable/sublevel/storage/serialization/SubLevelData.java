package dev.ryanhcode.sable.sublevel.storage.serialization;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A half-loaded sub-level data stored inside a chunk
 */
public final class SubLevelData {
    private final @NotNull UUID uuid;
    private final @NotNull BoundingBox3d bounds;
    private final @NotNull Pose3d pose;
    private final @NotNull List<UUID> relations;
    private final @NotNull CompoundTag fullTag;
    private ChunkPos originLoadedChunk = null;

    public SubLevelData(@NotNull final UUID uuid, @NotNull final BoundingBox3d bounds, @NotNull final Pose3d pose, @NotNull final List<UUID> relations, @NotNull final CompoundTag fullTag) {
        this.uuid = uuid;
        this.bounds = bounds;
        this.pose = pose;
        this.relations = relations;
        this.fullTag = fullTag;
    }

    public ChunkPos getOriginLoadedChunk() {
        return this.originLoadedChunk;
    }

    public void setOriginLoadedChunk(final ChunkPos originLoadedChunk) {
        this.originLoadedChunk = originLoadedChunk;
    }

    public @NotNull UUID uuid() {
        return this.uuid;
    }

    public @NotNull BoundingBox3d bounds() {
        return this.bounds;
    }

    public @NotNull Pose3d pose() {
        return this.pose;
    }

    public @NotNull List<UUID> dependencies() {
        return this.relations;
    }

    public @NotNull CompoundTag fullTag() {
        return this.fullTag;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final var that = (SubLevelData) obj;
        return Objects.equals(this.uuid, that.uuid) &&
                Objects.equals(this.bounds, that.bounds) &&
                Objects.equals(this.pose, that.pose) &&
                Objects.equals(this.relations, that.relations) &&
                Objects.equals(this.fullTag, that.fullTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uuid, this.bounds, this.pose, this.relations, this.fullTag);
    }

    @Override
    public String toString() {
        return "HalfLoadedSublevel[" +
                "uuid=" + this.uuid + ", " +
                "bounds=" + this.bounds + ", " +
                "pose=" + this.pose + ", " +
                "relations=" + this.relations + ']';
    }

}
