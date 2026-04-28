package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.mechanical_arm;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Optional;

/**
 * A point holder to record and gather information from ArmInteractionPoints
 *
 * @param pos The position of this holder
 * @param interactionMode the ArmInteractionPoint mode of this holder
 * @param covered Whether this point holder is being covered by an ArmInteractionPoint
 */
public record PointHolder(BlockPos pos, ArmInteractionPoint.Mode interactionMode, MutableBoolean covered) {

    public CompoundTag serialize(final BlockPos anchor) {
        final CompoundTag tag = new CompoundTag();
        final Tag pos = NbtUtils.writeBlockPos(this.pos.subtract(anchor));

        tag.put("pos", pos);
        NBTHelper.writeEnum(tag, "mode", this.interactionMode);

        return tag;
    }

    public static PointHolder deserialize(final CompoundTag tag, final BlockPos anchor) {
        final Optional<BlockPos> pos = NbtUtils.readBlockPos(tag, "pos");

        return pos.map(blockPos -> new PointHolder(blockPos.offset(anchor), NBTHelper.readEnum(tag, "mode", ArmInteractionPoint.Mode.class), new MutableBoolean(false))).orElse(null);

    }

}
