package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.platform.SableAssemblyPlatform;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SableAssemblyPlatformImpl implements SableAssemblyPlatform {

    @Override
    public void setIgnoreOnPlace(final Level level, final boolean ignore) {
        level.captureBlockSnapshots = ignore;
    }
}
