package dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;

import java.util.Optional;
import java.util.function.Function;

public class EntitySubLevelRotationHelper {

    public enum Type {
        CAMERA, ENTITY
    }
    public static boolean shouldCameraRotate() {
        return Minecraft.getInstance().options.getCameraType() != SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED;
    }

    /**
     * @return the current camera rotation quaternion from sub-level riding, stored in dest
     */
    @Nullable
    public static Quaterniond getEntityOrientation(final Entity cameraEntity, final Function<SubLevel, Pose3dc> poseProvider, final float partialTicks, final Type type) {
        final Quaterniond ridingOrientation = getSubLevelInheritedOrientation(cameraEntity, poseProvider, type);

        if (ridingOrientation != null) {
            return ridingOrientation;
        }

        final Quaterniondc entityOrientation = EntitySubLevelUtil.getCustomEntityOrientation(cameraEntity, partialTicks);

        if (entityOrientation != null) {
            return new Quaterniond(entityOrientation);
        }

        return null;

    }

    /**
     * @return the current camera rotation quaternion from sub-level riding, stored in dest
     */

    public static Quaterniond getSubLevelInheritedOrientation(final Entity cameraEntity, final Function<SubLevel, Pose3dc> poseProvider, final Type type) {
        if (type == Type.CAMERA && cameraEntity instanceof final Player player &&  player.isLocalPlayer() && !shouldCameraRotate()) {
            return null;
        }

        final ActiveSableCompanion helper = Sable.HELPER;
        if (cameraEntity instanceof final LivingEntity livingEntity && livingEntity.isSleeping()) {
            final Optional<BlockPos> sleepingPos = livingEntity.getSleepingPos();

            if (sleepingPos.isPresent()) {
                final BlockPos pos = sleepingPos.get();

                final SubLevel subLevel = helper.getContaining(livingEntity.level(), pos);
                if (subLevel instanceof final ClientSubLevel clientSubLevel) {
                    return new Quaterniond(clientSubLevel.renderPose().orientation());
                }
            }
        }

        if (cameraEntity == null) {
            return null;
        }

        Entity entity = cameraEntity.getVehicle();
        if (entity == null) {
            if(cameraEntity instanceof Player) {
                return null;
            } else if(helper.getContaining(cameraEntity) != null) {
                entity = cameraEntity;
            } else {
                return null;
            }
        }

        final SubLevel subLevel = helper.getContaining(entity);
        if (subLevel == null) {
            return null;
        }

        return new Quaterniond(poseProvider.apply(subLevel).orientation());
    }

}
