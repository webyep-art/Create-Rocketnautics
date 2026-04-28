package dev.ryanhcode.sable.sublevel.storage.holding;

public record SavedSubLevelPointer(short storageIndex, short subLevelIndex) {

    public int packed() {
        return (this.storageIndex << 16) | (this.subLevelIndex & 0xFFFF);
    }

    public static SavedSubLevelPointer unpack(final int packed) {
        final short storageIndex = (short) (packed >> 16);
        final short subLevelIndex = (short) (packed & 0xFFFF);
        return new SavedSubLevelPointer(storageIndex, subLevelIndex);
    }

    @Override
    public String toString() {
        return "local->[" +
                "storageIndex=" + this.storageIndex +
                ", subLevelIndex=" + this.subLevelIndex +
                ']';
    }
}
