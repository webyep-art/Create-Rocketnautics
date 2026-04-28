package dev.ryanhcode.sable.api.physics.constraint.rotary;

import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import org.joml.Vector3dc;

/**
 * A configuration for a rotary joint constraint, with a single angular DOF.
 * @param pos1 the position in world space assumed to be inside the plot of the first sub-level (ex. a block position).
 * @param pos2 the position in world space assumed to be inside the plot of the second sub-level (ex. a block positino).
 * @param normal1 the local normal of the joint on the first sub-level.
 * @param normal2 the local normal of the joint on the second sub-level.
 */
public record RotaryConstraintConfiguration(Vector3dc pos1, Vector3dc pos2, Vector3dc normal1, Vector3dc normal2) implements PhysicsConstraintConfiguration<RotaryConstraintHandle> {

}