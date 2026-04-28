package dev.ryanhcode.sable.sublevel.storage;

import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class HoldingSubLevel {
    private final @NotNull SubLevelData data;
    private GlobalSavedSubLevelPointer pointer;

    public HoldingSubLevel(@NotNull final SubLevelData data, final GlobalSavedSubLevelPointer pointer) {
        this.data = data;
        this.pointer = pointer;
    }

    public @NotNull SubLevelData data() {
        return this.data;
    }

    public GlobalSavedSubLevelPointer pointer() {
        return this.pointer;
    }

    public void setPointer(final GlobalSavedSubLevelPointer pointer) {
        this.pointer = pointer;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final var that = (HoldingSubLevel) obj;
        return Objects.equals(this.data, that.data) &&
                Objects.equals(this.pointer, that.pointer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data, this.pointer);
    }

    @Override
    public String toString() {
        return "HoldingSubLevel[" +
                "data=" + this.data + ", " +
                "pointer=" + this.pointer + ']';
    }

}
