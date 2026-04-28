package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.fabric.mixinterface.LevelExtension;
import dev.ryanhcode.sable.platform.SableAssemblyPlatform;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SableAssemblyPlatformImpl implements SableAssemblyPlatform {

    @Override
    public void setIgnoreOnPlace(final Level level, final boolean ignore) {
        ((LevelExtension) level).sable$setIgnoreOnPlace(ignore);
    }
}
