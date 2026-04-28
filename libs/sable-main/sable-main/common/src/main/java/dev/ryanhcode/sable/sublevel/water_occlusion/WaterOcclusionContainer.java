package dev.ryanhcode.sable.sublevel.water_occlusion;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.water_occlusion.WaterOcclusionContainerHolder;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Stores water occlusion regions for a level. Water occlusion regions are not networked.
 */
public abstract class WaterOcclusionContainer<T extends WaterOcclusionRegion> {

    /**
     * All water occlusion regions within this level
     */
    protected final Set<T> regions = new ObjectOpenHashSet<>();

    /**
     * The level this storage is for
     */
    private final Level level;

    public WaterOcclusionContainer(final Level level) {
        this.level = level;
    }

    /**
     * @param level the level
     * @return the water occlusion container in a level
     */
    public static @Nullable WaterOcclusionContainer<?> getContainer(final Level level) {
        if (level instanceof final WaterOcclusionContainerHolder holder) {
            return holder.sable$getWaterOcclusionContainer();
        }
        return null;
    }

    /**
     * Checks if a given location is in any water-occluded regions.
     *
     * @param location the location to check
     * @return if the location is in any regions of water occlusion
     */
    public boolean isOccluded(final Vec3 location) {
        ActiveSableCompanion helper = Sable.HELPER;
        for (final T region : this.regions) {
            final BoundedBitVolume3i bitSet = region.getVolume();
            final SubLevel subLevel = helper.getContaining(this.level, bitSet.getMinBlockPos());

            final Vec3 localLocation = subLevel != null ?
                    subLevel.logicalPose().transformPositionInverse(location)
                    : location;

            if (bitSet.getOccupied(Mth.floor(localLocation.x), Mth.floor(localLocation.y), Mth.floor(localLocation.z))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a given location is in any water-occluded regions.
     *
     * @param location the location to check
     * @return if the location is in any regions of water occlusion
     */
    @Nullable
    public T getOccludingRegion(final Vec3 location) {
        ActiveSableCompanion helper = Sable.HELPER;
        for (final T region : this.regions) {
            final BoundedBitVolume3i bitSet = region.getVolume();
            final SubLevel subLevel = helper.getContaining(this.level, bitSet.getMinBlockPos());

            final Vec3 localLocation = subLevel != null ?
                    subLevel.logicalPose().transformPositionInverse(location)
                    : location;

            if (bitSet.getOccupied(Mth.floor(localLocation.x), Mth.floor(localLocation.y), Mth.floor(localLocation.z))) {
                return region;
            }
        }

        return null;
    }

    public void markDirty(final BlockPos pos) {
        for (final T region : this.regions) {
            final BoundedBitVolume3i bitSet = region.getVolume();

            for (final Direction direction : Direction.values()) {
                final BlockPos rel = pos.relative(direction);
                if (bitSet.getOccupied(rel.getX(), rel.getY(), rel.getZ())) {
                    region.markDirty();
                    break;
                }
            }
        }
    }

    public abstract void removeRegion(final WaterOcclusionRegion region);

    public abstract WaterOcclusionRegion addRegion(final BoundedBitVolume3i region);

    public Set<T> getRegions() {
        return this.regions;
    }
}
