package dev.ryanhcode.sable.debug;

import net.minecraft.core.Direction;

import java.util.UUID;

public record GizmoSelection(UUID subLevel, Direction.Axis axis) {
}
