package dev.ryanhcode.sable.physics.impl.none;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.object.box.BoxHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

/**
 * A physics engine that does nothing, and keeps every sub-level static.
 */
public class StaticPhysicsPipeline implements PhysicsPipeline {

    @Override
    public void init(final Vector3dc gravity, final double universalDrag) {
        // no-op
    }

    @Override
    public void dispose() {
        // no-op
    }

    @Override
    public void prePhysicsTicks() {
        // no-op
    }

    @Override
    public void physicsTick(final double timeStep) {
        // no-op
    }

    @Override
    public void postPhysicsTicks() {
        // no-op
    }

    @Override
    public void tick() {
        // no-op
    }

    @Override
    public void add(final ServerSubLevel subLevel, final Pose3dc pose) {
        // no-op
    }

    @Override
    public void remove(final ServerSubLevel subLevel) {
        // no-op
    }

    @Override
    public void add(final KinematicContraption contraption) {

    }

    @Override
    public void remove(final KinematicContraption contraption) {

    }

    @Override
    public Pose3d readPose(final ServerSubLevel subLevel, final Pose3d dest) {
        return dest;
    }

    /**
     * Adds a rope to the physics pipeline
     */
    @Override
    public RopeHandle addRope(final RopePhysicsObject rope) {
        return null;
    }

    /**
     * Adds a box to the physics pipeline
     */
    @Override
    public BoxHandle addBox(final BoxPhysicsObject boxPhysicsObject) {
        return null;
    }

    @Override
    public void handleChunkSectionAddition(final LevelChunkSection chunk, final int x, final int y, final int z, final boolean uploadDataIfGlobal) {
        // no-op
    }

    @Override
    public void handleChunkSectionRemoval(final int x, final int y, final int z) {
        // no-op
    }

    @Override
    public void handleBlockChange(final SectionPos sectionPos, final LevelChunkSection chunk, final int x, final int y, final int z, final BlockState oldState, final BlockState newState) {
        // no-op
    }

    @Override
    public void teleport(final PhysicsPipelineBody body, final Vector3dc position, final Quaterniondc orientation) {
        if (body instanceof final ServerSubLevel subLevel) {
            subLevel.logicalPose().position().set(position);
            subLevel.logicalPose().orientation().set(orientation);
        }
    }

    @Override
    public void applyImpulse(final PhysicsPipelineBody body, final Vector3dc position, final Vector3dc force) {
        // no-op
    }

    @Override
    public void applyLinearAndAngularImpulse(final PhysicsPipelineBody body, final Vector3dc position, final Vector3dc torque, final boolean wakeUp) {
        // no-op
    }

    @Override
    public void wakeUp(final PhysicsPipelineBody body) {
        // no-op
    }

    @Override
    public int getNextRuntimeID() {
        return 0;
    }
}
