package dev.ryanhcode.sable.api.physics.mass;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.SableMathUtils;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.util.Collection;
import java.util.Objects;

public class MergedMassTracker implements MassData {
    private final MassTracker selfTracker;
    /**
     * The sub-level to track the merged mass of
     */
    private final ServerSubLevel subLevel;
    /**
     * The merged mass of the sub-level, including contraptions [kpg]
     */
    private double mass;
    /**
     * The merged inertia tensor of the sub-level, including contraptions [kgm^2]
     */
    private final Matrix3d inertiaTensor = new Matrix3d().zero();
    /**
     * 1 / merged mass of the sub-level, including contraptions [1 / kpg]
     */
    private double inverseMass;
    /**
     * 1 / merged inertia tensor of the sub-level, including contraptions [1 / kgm^2]
     */
    private final Matrix3d inverseInertiaTensor = new Matrix3d().zero();
    /**
     * The merged center of mass of the sub-level, including contraptions [m]
     */
    private @Nullable Vector3d centerOfMass;

    private double lastMass;
    private @Nullable Vector3d lastCenterOfMass;
    private @Nullable Matrix3d lastInertiaTensor;

    public MergedMassTracker(@NotNull final ServerSubLevel subLevel, final MassTracker selfTracker) {
        this.subLevel = subLevel;
        this.selfTracker = selfTracker;
    }

    /**
     * Updates the merged mass properties of this sub-level, and merges the contraption mass trackers into this one
     */
    public void update(final float partialPhysicsTick) {
        if (this.selfTracker.getCenterOfMass() == null) {
            return;
        }

        final Collection<KinematicContraption> contraptions = this.subLevel.getPlot().getContraptions();

        this.mass = this.selfTracker.getMass();
        this.centerOfMass = this.selfTracker.getCenterOfMass().mul(this.getMass(), new Vector3d());

        for (final KinematicContraption contraption : contraptions) {
            final MassTracker contraptionMassData = contraption.sable$getMassTracker();
            this.mass = this.getMass() + contraptionMassData.getMass();
            this.centerOfMass.fma(contraptionMassData.getMass(), contraption.sable$getPosition(partialPhysicsTick));
        }

        this.centerOfMass.mul(1 / this.getMass());

        this.inertiaTensor.set(this.selfTracker.getInertiaTensor());
        final Vector3d localShift = this.centerOfMass.sub(this.selfTracker.getCenterOfMass(), new Vector3d());

        // nudge inertia tensor
        SableMathUtils.fmaInertiaTensor(localShift, this.selfTracker.getMass(), this.inertiaTensor);

        for (final KinematicContraption contraption : contraptions) {
            final MassTracker contraptionMassData = contraption.sable$getMassTracker();

            final Vector3d localPos = contraption.sable$getPosition(partialPhysicsTick).sub(this.centerOfMass, new Vector3d());
            SableMathUtils.fmaInertiaTensor(localPos, contraptionMassData.getMass(), this.inertiaTensor);

            final Quaterniond contraptionOrientation = contraption.sable$getOrientation(partialPhysicsTick);

            // Q * (I * (Q^-1 * v))
            final Matrix3d localInertiaTensor = new Matrix3d()
                    .rotateLocal(contraptionOrientation.conjugate(new Quaterniond()))
                    .mulLocal(contraptionMassData.getInertiaTensor())
                    .rotateLocal(contraptionOrientation);

            this.inertiaTensor.add(localInertiaTensor);
        }

        this.inverseMass = 1.0 / this.mass;
        this.inertiaTensor.invert(this.inverseInertiaTensor);

        this.uploadData();
        this.setPreviousValues();
    }

    private void uploadData() {
        if (this.centerOfMass != null && (this.mass != this.lastMass ||
                !Objects.equals(this.lastCenterOfMass, this.centerOfMass) ||
                !Objects.equals(this.lastInertiaTensor, this.inertiaTensor))) {
            if (this.lastCenterOfMass == null || this.lastInertiaTensor == null) {
                this.lastCenterOfMass = new Vector3d(this.centerOfMass);
                this.lastInertiaTensor = new Matrix3d(this.inertiaTensor);
            }

            final ServerLevel level = this.subLevel.getLevel();
            final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

            final Vector3d movement = this.centerOfMass.sub(this.lastCenterOfMass, new Vector3d());

            physicsSystem.updatePose(this.subLevel);
            final Pose3d pose = this.subLevel.logicalPose();
            physicsSystem.getPipeline().teleport(this.subLevel, pose.position().add(pose.orientation().transform(movement)), pose.orientation());
            pose.rotationPoint().set(this.centerOfMass);
            physicsSystem.getPipeline().onStatsChanged(this.subLevel);
        }
    }

    private void setPreviousValues() {
        if (this.centerOfMass == null) {
            this.lastCenterOfMass = null;
            this.lastInertiaTensor = null;
        } else {
            if (this.lastCenterOfMass == null) {
                this.lastCenterOfMass = new Vector3d();
                this.lastInertiaTensor = new Matrix3d().zero();
            }
            this.lastCenterOfMass.set(this.centerOfMass);
            this.lastInertiaTensor.set(this.inertiaTensor);
        }

        this.lastMass = this.mass;
    }

    @Override
    public double getInverseMass() {
        return this.inverseMass;
    }

    @Override
    public Matrix3dc getInverseInertiaTensor() {
        return this.inverseInertiaTensor;
    }

    @Override
    public Matrix3dc getInertiaTensor() {
        return this.inertiaTensor;
    }

    @Override
    public double getMass() {
        return this.mass;
    }

    @Override
    public Vector3dc getCenterOfMass() {
        return this.centerOfMass;
    }

    /**
     * @return the mass tracker for just the sub-level, not including merged masses
     */
    public MassTracker getSelfMassTracker() {
        return this.selfTracker;
    }
}
