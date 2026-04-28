package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.platform.SablePlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class SablePlatformImpl implements SablePlatform {

    @Override
    public boolean isWrappedLevel(@Nullable final Level level) {
        return false;
    }

    @Override
    public boolean isBlockstateLadder(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
        return false; //handled already for fabric
    }
}
