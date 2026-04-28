package dev.ryanhcode.sable.api.physics.constraint;

/**
 * All degrees of freedom that joint motors and locks can be imposed on
 */
public enum ConstraintJointAxis {
    LINEAR_X,
    LINEAR_Y,
    LINEAR_Z,
    ANGULAR_X,
    ANGULAR_Y,
    ANGULAR_Z;

    public static final ConstraintJointAxis[] ALL = values();
    public static final ConstraintJointAxis[] LINEAR = {LINEAR_X, LINEAR_Y, LINEAR_Z};
    public static final ConstraintJointAxis[] ANGULAR = {ANGULAR_X, ANGULAR_Y, ANGULAR_Z};
}
