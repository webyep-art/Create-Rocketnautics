package dev.ryanhcode.sable.util;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.joml.Math;

public class SableMathUtils {
    private static final Vector3d temp = new Vector3d();
    private static final Quaterniondc[] ALL_QUATS = new Quaterniondc[]{
            new Quaterniond(0, 0, 0, 1),
            new Quaterniond(1, 0, 0, 0),
            new Quaterniond(0, 1, 0, 0),
            new Quaterniond(0, 0, 1, 0),
            new Quaterniond(1, 0, 0, 1).normalize(),
            new Quaterniond(0, 1, 0, 1).normalize(),
            new Quaterniond(0, 0, 1, 1).normalize(),
            new Quaterniond(0, 1, 1, 0).normalize(),
            new Quaterniond(1, 0, 1, 0).normalize(),
            new Quaterniond(1, 1, 0, 0).normalize(),
            new Quaterniond(1, 1, 1, 1).normalize()
    };

    public static Matrix3d setOuterProduct(final Vector3dc u, final Vector3dc v, final Matrix3d target) {
        target.m00 = u.x() * v.x();
        target.m01 = u.y() * v.x();
        target.m02 = u.z() * v.x();
        target.m10 = u.x() * v.y();
        target.m11 = u.y() * v.y();
        target.m12 = u.z() * v.y();
        target.m20 = u.x() * v.z();
        target.m21 = u.y() * v.z();
        target.m22 = u.z() * v.z();
        return target;
    }

    public static Matrix3d setOuterProduct(final Vector3dc u, final Vector3dc v, final double scale, final Matrix3d target) {
        target.m00 = u.x() * v.x() * scale;
        target.m01 = u.y() * v.x() * scale;
        target.m02 = u.z() * v.x() * scale;
        target.m10 = u.x() * v.y() * scale;
        target.m11 = u.y() * v.y() * scale;
        target.m12 = u.z() * v.y() * scale;
        target.m20 = u.x() * v.z() * scale;
        target.m21 = u.y() * v.z() * scale;
        target.m22 = u.z() * v.z() * scale;
        return target;
    }

    public static Matrix3d addOuterProduct(final Vector3dc u, final Vector3dc v, final Matrix3d target) {
        target.m00 += u.x() * v.x();
        target.m01 += u.y() * v.x();
        target.m02 += u.z() * v.x();
        target.m10 += u.x() * v.y();
        target.m11 += u.y() * v.y();
        target.m12 += u.z() * v.y();
        target.m20 += u.x() * v.z();
        target.m21 += u.y() * v.z();
        target.m22 += u.z() * v.z();
        return target;
    }

    public static Matrix3d fmaOuterProduct(final Vector3dc u, final Vector3dc v, final double scale, final Matrix3d target) {
        target.m00 += u.x() * v.x() * scale;
        target.m01 += u.y() * v.x() * scale;
        target.m02 += u.z() * v.x() * scale;
        target.m10 += u.x() * v.y() * scale;
        target.m11 += u.y() * v.y() * scale;
        target.m12 += u.z() * v.y() * scale;
        target.m20 += u.x() * v.z() * scale;
        target.m21 += u.y() * v.z() * scale;
        target.m22 += u.z() * v.z() * scale;
        return target;
    }

    /**
     * equivalent to identity.scale(u.dot(u))-fmaOuterProduct(u,u)
     */
    public static Matrix3d fmaInertiaTensor(final Vector3dc u, final double scale, final Matrix3d target) {
        target.m00 += (u.y() * u.y() + u.z() * u.z()) * scale;
        target.m01 -= u.y() * u.x() * scale;
        target.m02 -= u.z() * u.x() * scale;
        target.m10 -= u.x() * u.y() * scale;
        target.m11 += (u.z() * u.z() + u.x() * u.x()) * scale;
        target.m12 -= u.z() * u.y() * scale;
        target.m20 -= u.x() * u.z() * scale;
        target.m21 -= u.y() * u.z() * scale;
        target.m22 += (u.x() * u.x() + u.y() * u.y()) * scale;
        return target;
    }

    public static double multiplyInnerProduct(final Vector3dc u, final Matrix3dc A, final Vector3dc v) {
        A.transform(v, temp);
        return temp.dot(u);
    }
    static final Quaterniond difference = new Quaterniond();
    public static Vector3d getAngularVelocity(final Quaterniondc lastOrientation, final Quaterniondc orientation, final Vector3d dest) {
        orientation.difference(lastOrientation, difference).conjugate();

        if(difference.w<0)
            difference.mul(-1);

        final Vector3d angularVelocity = dest.set(difference.x, difference.y, difference.z);

        if (angularVelocity.lengthSquared() <= 1E-15)
            angularVelocity.mul(2.0 / difference.w);
        else {
            angularVelocity.normalize().mul(2.0 * Math.safeAcos(difference.w));
        }

        return dest;
    }

    public static Quaterniond clampQuaternionToGrid(final Quaterniondc q, final Iterable<Quaterniondc> gridQuats, final Quaterniond dest) {
        //negative of sign of each component of q
        final int signX = q.x() < 0 ? -1 : 1;
        final int signY = q.y() < 0 ? -1 : 1;
        final int signZ = q.z() < 0 ? -1 : 1;
        final int signW = q.w() < 0 ? -1 : 1;

        dest.set(q);
        //enforce q to only have non-positive entries, so that adding behaves like subtraction
        dest.x *= -signX;
        dest.y *= -signY;
        dest.z *= -signZ;
        dest.w *= -signW;

        final Quaterniond temp = new Quaterniond();
        final Quaterniond best = new Quaterniond();
        double distance = 10;

        for (final Quaterniondc gq : gridQuats) {
            final double currentDist = dest.add(gq, temp).lengthSquared();
            if (currentDist < distance) {
                distance = currentDist;
                best.set(gq);
            }
        }

        dest.set(best);
        dest.x *= signX;
        dest.y *= signY;
        dest.z *= signZ;
        dest.w *= signW;
        return dest;
    }

    /**
     * @param massData the mass-tracker for the sub-level being damped
     * @param frictionForce the desired friction force
     * @param frictionTorque the desired friction torque
     * @param localLinearVelocity the current local linear velocity of the sub-level
     * @param localAngularVelocity the current local angular velocity of the sub-level
     * @param timeStep the time-step to apply the force over
     * @param forceTotal the force total to store the resulting damping force into
     */
    public static void dampSubLevel(final MassData massData,
                                    final Vector3d frictionForce,
                                    final Vector3d frictionTorque,
                                    final Vector3dc localLinearVelocity,
                                    final Vector3dc localAngularVelocity,
                                    final double timeStep,
                                    final ForceTotal forceTotal) {
        final Vector3d expectedVelocity = new Vector3d();
        expectedVelocity.set(frictionForce);
        expectedVelocity.mul(massData.getInverseMass());
        expectedVelocity.mul(timeStep);
        final double forceScale = getClampingFactor(localLinearVelocity, expectedVelocity);

        expectedVelocity.set(frictionTorque);
        massData.getInverseInertiaTensor().transform(expectedVelocity);
        expectedVelocity.mul(timeStep);
        final double torqueScale = getClampingFactor(localAngularVelocity, expectedVelocity);

        frictionForce.mul(forceScale);
        frictionTorque.mul(torqueScale);
        forceTotal.applyLinearAndAngularImpulse(frictionForce, frictionTorque);
    }

    private static double getClampingFactor(final Vector3dc currentVelocity, final Vector3dc expectedVelocityChange) {
        final double k = -currentVelocity.dot(expectedVelocityChange);
        final double v = currentVelocity.lengthSquared();
        if (k < 0) // don't apply friction that increases velocity
            return 0;
        if (10 * k < v)  // if the expected velocity is 10 times smaller than the actual velocity, dont bother with clamping it
            return 1 - k / (2 * v);
        if (v < 1E-10) // simpler clamping for tiny values to avoid inaccuracies and numerical explosion
            return v / (k + 1E-10);
        return v * (1 - java.lang.Math.exp(-k / v)) / k;
    }

    public enum GridQuats implements ObjectIterable<Quaterniondc> {
        ALL(0b11111111111),
        X_AXIS(0b10011),
        Y_AXIS(0b100101),
        Z_AXIS(0b1001001),
        REAL(0b10001110001);

        private final ObjectList<Quaterniondc> currentQuats = new ObjectArrayList<>(ALL_QUATS.length);
        private final ObjectList<Quaterniondc> oppositeQuats = new ObjectArrayList<>(ALL_QUATS.length);

        GridQuats(int bitPattern) {
            for (final Quaterniondc q : ALL_QUATS) {
                (((bitPattern & 1) > 0) ? this.currentQuats : this.oppositeQuats).add(q);
                bitPattern >>= 1;
            }
        }

        public ObjectIterable<Quaterniondc> opposite() {
            return this.oppositeQuats::iterator;
        }

        @Override
        public @NotNull ObjectIterator<Quaterniondc> iterator() {
            return this.currentQuats.iterator();
        }
    }
}
