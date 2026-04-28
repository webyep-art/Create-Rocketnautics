package dev.ryanhcode.sable.fabric.mixin.assembly;

import dev.ryanhcode.sable.fabric.mixinterface.LevelExtension;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public class LevelMixin implements LevelExtension {

    @Unique
    private boolean sable$ignoreOnPlace;

    @Override
    public boolean sable$isIgnoreOnPlace() {
        return this.sable$ignoreOnPlace;
    }

    @Override
    public void sable$setIgnoreOnPlace(final boolean ignore) {
        this.sable$ignoreOnPlace = ignore;
    }
}
