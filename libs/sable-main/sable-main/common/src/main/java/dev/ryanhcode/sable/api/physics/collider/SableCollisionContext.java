package dev.ryanhcode.sable.api.physics.collider;

import dev.ryanhcode.sable.physics.impl.SableCollisionContextImpl;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Context used for getting collision shapes for sable physics pipelines.
 */
public interface SableCollisionContext extends CollisionContext {

    /**
     * @return The collision context to use for getting shapes
     */
    static SableCollisionContext get() {
        return SableCollisionContextImpl.INSTANCE;
    }
}
