package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface for sub-classes of {@link net.minecraft.world.level.block.entity.BlockEntity} to implement behaviour
 * when mounted on a sub-level.
 */
public interface BlockEntitySubLevelActor {

    /**
     * Called once per server game tick when this actor is on a {@link SubLevel}
     */
    default void sable$tick(final ServerSubLevel subLevel) {}

    /**
     * Called once per **physics** tick when this actor is on a {@link SubLevel}.
     * There may be multiple physics ticks per tick.
     *
     * @param subLevel the sub-level this block entity is on
     * @param timeStep the time this physics tick is stepping
     */
    default void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {}

    /**
     * Returns the loading dependencies this block-entity has on other sub-levels.
     * Loading dependencies are used to unload and load a group of sub-levels together.
     * By default, loading dependencies are assumed from the connection dependencies.
     * <p>
     * Note that this may be called after chunks have been un-loaded, and as such, direct level access
     * should not be done to fetch the dependencies.
     *
     * @return a collection of loading dependencies on other loaded sub-levels, or null for none
     */
    @Nullable
    default Iterable<@NotNull SubLevel> sable$getLoadingDependencies() {
        return this.sable$getConnectionDependencies();
    }

    /**
     * Returns the connections this block-entity has on other sub-levels.
     * Connections are used to dictate sub-levels that should be treated as one by many systems.
     * <p>
     * Note that this may be called after chunks have been un-loaded, and as such, direct level access
     * should not be done to fetch the dependencies.
     * @return a collection of connection dependencies on other loaded sub-levels, or null for none
     */
    @Nullable
    default Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        return null;
    }
}
