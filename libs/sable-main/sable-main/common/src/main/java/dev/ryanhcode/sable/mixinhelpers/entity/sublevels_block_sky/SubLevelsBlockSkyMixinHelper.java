package dev.ryanhcode.sable.mixinhelpers.entity.sublevels_block_sky;

import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SubLevelsBlockSkyMixinHelper {

    @ApiStatus.Internal
    public static boolean checkSkyWithSublevels(final Level level, final BlockPos pos) {
        final Vec3 start = Vec3.atBottomCenterOf(pos);

        final ClipContext context = new ClipContext(
                start,
                new Vec3(start.x, level.getMaxBuildHeight(), start.z),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                CollisionContext.empty()
        );

        ((ClipContextExtension) context).sable$setIgnoreMainLevel(true);

        return level.clip(context).getType() != HitResult.Type.MISS;
    }

}
