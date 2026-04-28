package dev.ryanhcode.sable.api.physics.constraint.free;

import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

/**
 * A configuration for a free constraint, which imposes no locks
 *
 * @param pos1 the position in world space assumed to be inside the plot of the first sub-level (ex. a block position).
 * @param pos2 the position in world space assumed to be inside the plot of the second sub-level (ex. a block position).
 * @param orientation the local orientation of the second body from the first. Motor axes will be relative to this frame
 */
public record FreeConstraintConfiguration(Vector3dc pos1, Vector3dc pos2, Quaterniondc orientation) implements PhysicsConstraintConfiguration<FreeConstraintHandle> {

}