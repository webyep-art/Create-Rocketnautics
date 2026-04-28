package dev.ryanhcode.sable.api.physics.mass;

import dev.ryanhcode.sable.api.block.BlockSubLevelCustomCenterOfMass;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.util.SableMathUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.function.BiFunction;

/**
 * Tracks the mass / inertia tensor of a structure
 */
public class MassTracker implements MassData {
    private static final AABB UNIT_BOUNDS = new AABB(0, 0, 0, 1, 1, 1);

    /**
     * The memoized internal center of masses for block-states
     */
    public static BiFunction<BlockGetter, BlockState, Vector3dc> BLOCK_CENTER_OF_MASS = new BiFunction<>() {
        private final Int2ObjectOpenHashMap<Vector3dc> cache = new Int2ObjectOpenHashMap<>();

        @Override
        public Vector3dc apply(final BlockGetter blockGetter, final BlockState state) {
            return this.cache.computeIfAbsent(state.hashCode(), x -> {
                if (state.isAir()) {
                    return JOMLConversion.HALF;
                }

                if (state.getBlock() instanceof final BlockSubLevelCustomCenterOfMass customCenterOfMass) {
                    return customCenterOfMass.getCenterOfMass(blockGetter, state);
                }

                final VoxelShape shape = state.getCollisionShape(blockGetter, BlockPos.ZERO);

                if (shape.isEmpty()) {
                    return JOMLConversion.HALF;
                }

                if (state.isCollisionShapeFullBlock(blockGetter, BlockPos.ZERO)) {
                    return JOMLConversion.HALF;
                }

                final AABB bounds = shape.bounds().intersect(UNIT_BOUNDS);
                return JOMLConversion.toJOML(bounds.getCenter());
            });
        }
    };

    private static final Matrix3d BLOCK_INERTIA = new Matrix3d();
    /**
     * The mass of the sub-level [kpg]
     */
    private double mass;
    /**
     * The inertia tensor of the sub-level [kgm^2]
     */
    private Matrix3d inertiaTensor;
    /**
     * 1 / mass of the sub-level [1 / kpg]
     */
    private double inverseMass;
    /**
     * 1 / inertia tensor of the sub-level [1 / kgm^2]
     */
    private Matrix3d inverseInertiaTensor;
    /**
     * The center of mass of the sub-level [m]
     */
    private @Nullable Vector3d centerOfMass;

    /**
     * The data to track the mass of
     */
    public MassTracker() {
        this.mass = 0.0;
        this.centerOfMass = null;
        this.inertiaTensor = new Matrix3d().zero();
        this.inverseInertiaTensor = new Matrix3d().zero();
    }

    /**
     * Creates a new mass tracker for a sub-level.
     */
    public static MassTracker build(final BlockGetter blockGetter, final BoundingBox3ic bounds) {
        double mass = 0.0;
        final Vector3d centerOfMass = new Vector3d();
        final Matrix3d inertiaTensor = new Matrix3d().zero();

        final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        final Vector3d blockCenter = new Vector3d();
        int blockCount = 0;

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    final BlockState state = blockGetter.getBlockState(blockPos.set(x, y, z));

                    if (!VoxelNeighborhoodState.isSolid(blockGetter, blockPos, state)) {
                        continue;
                    }

                    final double blockMass = PhysicsBlockPropertyHelper.getMass(blockGetter, blockPos, state);
                    blockCenter.set(x, y, z)
                            .add(BLOCK_CENTER_OF_MASS.apply(blockGetter, state));

                    mass += blockMass;
                    centerOfMass.fma(blockMass, blockCenter);
                    blockCount++;
                }
            }
        }

        if (blockCount == 0) {
            final MassTracker tracker = new MassTracker();
            tracker.mass = 0.0;
            tracker.centerOfMass = null;
            tracker.inertiaTensor = new Matrix3d().zero();
            tracker.inverseInertiaTensor = new Matrix3d().zero();
            return tracker;
        }

        centerOfMass.div(mass);

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    final BlockState state = blockGetter.getBlockState(blockPos.set(x, y, z));

                    if (!VoxelNeighborhoodState.isSolid(blockGetter, blockPos, state)) {
                        continue;
                    }

                    blockCenter.set(x, y, z)
                            .add(BLOCK_CENTER_OF_MASS.apply(blockGetter, state));

                    final double blockMass = PhysicsBlockPropertyHelper.getMass(blockGetter, blockPos, state);
                    final Vec3 blockInertia = PhysicsBlockPropertyHelper.getInertia(blockGetter, blockPos, state);
                    final Vector3d r = blockCenter.sub(centerOfMass);

                    MassTracker.addBlockInertia(r, blockMass, inertiaTensor, blockInertia);
                }
            }
        }

        final Matrix3d inverseInertiaTensor = new Matrix3d(inertiaTensor).invert();
        final double inverseMass = 1.0 / mass;

        final MassTracker tracker = new MassTracker();

        tracker.centerOfMass = centerOfMass;
        tracker.mass = mass;
        tracker.inverseMass = inverseMass;
        tracker.inertiaTensor = inertiaTensor;
        tracker.inverseInertiaTensor = inverseInertiaTensor;

        return tracker;
    }

    private static Matrix3d addBlockInertia(final Vector3d blockPos, final double blockMass, final Matrix3d dest, final @Nullable Vec3 blockInertia) {
        if (blockInertia == null) {
            // block doesn't specify inertia, we assume it to be a cube
            BLOCK_INERTIA.identity().scale(blockMass / 6.0);
        } else {
            // block specifies inertia, we use it as diagonals
            BLOCK_INERTIA.identity();
            BLOCK_INERTIA.m00 = blockInertia.x * blockMass;
            BLOCK_INERTIA.m11 = blockInertia.y * blockMass;
            BLOCK_INERTIA.m22 = blockInertia.z * blockMass;
        }

        dest.add(BLOCK_INERTIA);
        SableMathUtils.fmaInertiaTensor(blockPos, blockMass, dest);
        return dest;
    }

    /**
     * Adds the mass of a 1x1x1 cube to the sub-level.
     * Negative mass is equivalent to the removal of a block.
     *
     * @param blockPos  The position of the block
     * @param blockMass The mass of the block [kpg]
     */
    public void addBlockMass(final BlockGetter blockGetter, final BlockState state, final BlockPos blockPos, final double blockMass, final @Nullable Vec3 blockInertia) {
        final double oldMass = this.mass;
        final double newMass = oldMass + blockMass;

        final Vector3d blockCenter = new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                .add(BLOCK_CENTER_OF_MASS.apply(blockGetter, state));

        if (this.centerOfMass == null) {
            this.centerOfMass = new Vector3d(blockCenter);
        }

        final Vector3d blockCenterFromCOM = new Vector3d(blockCenter).sub(this.centerOfMass);

        addBlockInertia(blockCenterFromCOM, blockMass, this.inertiaTensor, blockInertia);
        this.mass = newMass;
        this.inverseMass = 1.0 / newMass;

        this.moveCenterOfMass(new Vector3d(this.centerOfMass).mul(oldMass).add(blockCenter.mul(blockMass)).div(newMass));
    }

    /**
     * Moves the center of mass to a new position.
     *
     * @param newCenterOfMass The new center of mass
     */
    public void moveCenterOfMass(final Vector3d newCenterOfMass) {
        final Vector3d diff = new Vector3d(newCenterOfMass).sub(this.centerOfMass);
        final Matrix3d outerProduct = new Matrix3d(
                diff.x * diff.x,
                diff.y * diff.x,
                diff.z * diff.x,

                diff.x * diff.y,
                diff.y * diff.y,
                diff.z * diff.y,

                diff.x * diff.z,
                diff.y * diff.z,
                diff.z * diff.z
        );

        final Matrix3d inertia = new Matrix3d().scale(diff.lengthSquared()).sub(outerProduct).scale(this.mass);

        this.inertiaTensor.sub(inertia);
        this.inverseInertiaTensor = new Matrix3d(this.inertiaTensor).invert();
        this.centerOfMass.set(newCenterOfMass);
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
}
