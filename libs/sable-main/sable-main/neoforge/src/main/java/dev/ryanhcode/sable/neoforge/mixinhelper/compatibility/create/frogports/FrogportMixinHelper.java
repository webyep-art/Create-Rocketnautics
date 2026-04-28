package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.frogports;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class FrogportMixinHelper {

    private FrogportMixinHelper() {
    }

    public static Vec3 getExactTargetLocation(final PackagePortTarget instance,
                                              final PackagePortBlockEntity packagePortBlockEntity,
                                              final LevelAccessor levelAccessor,
                                              final BlockPos blockPos,
                                              final Operation<Vec3> original) {
        final ActiveSableCompanion helper = Sable.HELPER;
        final Vec3 originalTarget = original.call(instance, packagePortBlockEntity, levelAccessor, blockPos);
        final Level level = packagePortBlockEntity.getLevel();
        final Vec3 globalTarget = helper.projectOutOfSubLevel(level, originalTarget);

        final SubLevel subLevel = helper.getContaining(level, packagePortBlockEntity.getBlockPos());

        Vec3 localTarget = globalTarget;

        if (subLevel != null) {
            localTarget = subLevel.logicalPose().transformPositionInverse(localTarget);
        }

        return localTarget;
    }


}
