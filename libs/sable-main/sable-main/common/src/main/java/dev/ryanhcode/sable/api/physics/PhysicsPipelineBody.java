package dev.ryanhcode.sable.api.physics;

import dev.ryanhcode.sable.api.physics.mass.MassData;

/**
 * A rigid-body tracked by a {@link PhysicsPipeline}
 */
public interface PhysicsPipelineBody {

    int NULL_RUNTIME_ID = -1;

    /**
     * The runtime integer ID tracked by the {@link PhysicsPipeline}
     */
    int getRuntimeId();

    /**
     * @return the mass data for this physics body
     */
    MassData getMassTracker();

    /**
     * @return if this body has been removed by the pipeline
     */
    boolean isRemoved();
}
