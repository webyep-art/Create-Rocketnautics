package dev.ryanhcode.sable.mixin.impact;

import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.callback.ExplosiveBlockCallback;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes tnt explode on high velocity collisions.
 * TODO: This process should be refined for adding collision callbacks to blocks.
 *  A mixin should not be absolutely necessary.
 */
@Mixin(TntBlock.class)
public abstract class TntBlockMixin implements BlockWithSubLevelCollisionCallback {

    @Override
    public BlockSubLevelCollisionCallback sable$getCallback() {
        return ExplosiveBlockCallback.INSTANCE;
    }

}
