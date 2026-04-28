package dev.ryanhcode.sable.sublevel.storage;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.BitSet;

/**
 * Stores the map for which plots are occupied
 */
public class SubLevelOccupancySavedData extends SavedData {
    public static final String FILE_ID = "sable_sub_level_occupancy";
    private final ServerLevel level;

    private SubLevelOccupancySavedData(final ServerLevel level) {
        this.level = level;
    }

    public static SubLevelOccupancySavedData getOrLoad(final ServerLevel level) {
        return level.getChunkSource().getDataStorage().computeIfAbsent(
                new Factory<>(
                        () -> new SubLevelOccupancySavedData(level),
                        (tag, provider) -> SubLevelOccupancySavedData.load(level, tag),
                        DataFixTypes.LEVEL
                ),
                SubLevelOccupancySavedData.FILE_ID);
    }


    private static SubLevelOccupancySavedData load(final ServerLevel level, final CompoundTag tag) {
        final SubLevelOccupancySavedData data = new SubLevelOccupancySavedData(level);

        final long[] longArray = tag.getLongArray("sub_level_occupancy");

        if (longArray.length > 0) {
            final BitSet occupancyData = BitSet.valueOf(longArray);
            final SubLevelContainer container = SubLevelContainer.getContainer(level);
            assert container != null : "Sub-level container is null";

            // clone into the container
            final BitSet occupancy = container.getOccupancy();
            occupancy.clear();
            occupancy.or(occupancyData);
        }

        return data;
    }

    @Override
    public CompoundTag save(final CompoundTag compoundTag, final HolderLookup.Provider provider) {
        final SubLevelContainer container = SubLevelContainer.getContainer(this.level);
        assert container != null : "Sub-level container is null";

        final BitSet occupancy = container.getOccupancy();

        final long[] longArray = occupancy.toLongArray();

        compoundTag.putLongArray("sub_level_occupancy", longArray);

        return compoundTag;
    }
}