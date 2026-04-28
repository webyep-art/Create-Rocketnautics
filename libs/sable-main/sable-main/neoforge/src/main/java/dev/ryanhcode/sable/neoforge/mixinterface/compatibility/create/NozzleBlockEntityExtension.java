package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create;

import net.minecraft.core.Direction;

import java.util.EnumSet;

public interface NozzleBlockEntityExtension {
	EnumSet<Direction> sable$getValidDirections();
}
