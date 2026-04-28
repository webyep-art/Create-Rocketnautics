package dev.ryanhcode.sable.physics;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.Map;

public class ReactionWheelManager {
    private static final Vector3d totalLocalAngularMomentum = new Vector3d();
    private static final Vector3d temp = new Vector3d();
    private final ServerSubLevel subLevel;
    private final Vector3d previousAngularMomentum = new Vector3d();
    private final ForceTotal forceTotal = new ForceTotal();

    public ReactionWheelManager(final ServerSubLevel subLevel) {
        this.subLevel = subLevel;
    }

    public void physicsTick(final RigidBodyHandle handle) {
        if (!this.needsTicking())
            return;

        totalLocalAngularMomentum.zero();
        for (final Map.Entry<BlockPos, BlockEntitySubLevelReactionWheel> wheelEntry : this.subLevel.getPlot().getBlockEntityReactionWheelMap()) {

            final BlockEntitySubLevelReactionWheel wheel = wheelEntry.getValue();
            final BlockPos pos = wheelEntry.getKey();
            this.addWheelMomentumToLocalVector(pos, wheel, totalLocalAngularMomentum);
        }
        this.subLevel.logicalPose().orientation().transform(totalLocalAngularMomentum);
        final Vector3d impulse = totalLocalAngularMomentum.sub(this.previousAngularMomentum, temp);

        this.subLevel.logicalPose().orientation().transformInverse(impulse);
        this.forceTotal.applyAngularImpulse(impulse);
        handle.applyForcesAndReset(this.forceTotal);

        this.previousAngularMomentum.set(totalLocalAngularMomentum);
    }

    public boolean needsTicking() {
        return this.previousAngularMomentum.lengthSquared() > 0 || !this.subLevel.getPlot().getBlockEntityReactionWheels().isEmpty();
    }

    public void wheelChanged(final BlockPos pos, final BlockEntitySubLevelReactionWheel wheel, final boolean add) {
        this.addWheelMomentumToLocalVector(pos, wheel, totalLocalAngularMomentum.zero());
        this.subLevel.logicalPose().orientation().transform(totalLocalAngularMomentum);
        if (add)
            this.previousAngularMomentum.add(totalLocalAngularMomentum);
        else
            this.previousAngularMomentum.sub(totalLocalAngularMomentum);
    }

    void addWheelMomentumToLocalVector(final BlockPos pos, final BlockEntitySubLevelReactionWheel wheel, final Vector3d v) {
        wheel.sable$getAngularVelocity(temp.zero());
        final Vec3 blockInertia = PhysicsBlockPropertyHelper.getInertia(this.subLevel.getLevel(), pos, wheel.getBlockState());
        if (blockInertia == null)
            temp.mul(1 / 6.0);//default block inertia
        else
            temp.mul(blockInertia.x, blockInertia.y, blockInertia.z);
        v.fma(PhysicsBlockPropertyHelper.getMass(this.subLevel.getLevel(), pos, wheel.getBlockState()), temp);
    }
}
