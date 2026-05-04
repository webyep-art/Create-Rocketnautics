package dev.devce.rocketnautics.content.orbit;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.SkyDataHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * A cuboid planet definition.
 */
public interface CubePlanetDefinition extends PointGravitySource {

    /**
     * The local space dimension tied to this planet. If null, getting too close to this planet will
     * send the rocket back to its last visited local space dimension.
     */
    @Nullable ResourceKey<Level> localSpaceDimension();

    /**
     * Used to provide render data for this cube planet. The render data will be projected on all six sides of the cube.
     * @param powerSizeClamp the maximum power size of the render data. See {@link SkyDataHandler#getRenderDataForDeepSpace(int)}
     * @return the render data, capped at the provided power size.
     */
    default byte[] renderData(int powerSizeClamp) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            RocketNautics.LOGGER.error("Attempted to load render data for a cube planet on the client. This must be queried from the server!");
            return new byte[256 * 256];
        }
        ResourceKey<Level> dim = localSpaceDimension();
        ServerLevel level = null;
        if (dim != null) {
            level = ServerLifecycleHooks.getCurrentServer().getLevel(dim);
        }
        if (level == null) {
            RocketNautics.LOGGER.warn("Attempted default load of render data for a cube planet that had no dimension. Such a planet should have a custom override for providing render data!");
            return new byte[256 * 256];
        }
        return SkyDataHandler.getHandlerForLevel(level).getRenderDataForDeepSpace(powerSizeClamp);
    }

    /**
     * The radius of the planet in meters. Note that this does not need to be correlated to the
     * dimension radius of 30,000,000 blocks; when transferring from deep space to local space,
     * a rocket will be placed the same proportion far out.
     * <br><br>
     * For example, consider a planet of radius 1,000,000. If a rocket leaves deep space at a
     * position corresponding to 900,000 meters out, they will be placed 27,000,000 blocks out
     * in the local space dimension.
     */
    double radius();

    /**
     * A quaternion describing the rotation of the planet at a given universe time.
     * @param universeTime the target universe time, in ticks. Accounts for time dilation.
     */
    Quaterniond rotationAtTime(long universeTime);
}
