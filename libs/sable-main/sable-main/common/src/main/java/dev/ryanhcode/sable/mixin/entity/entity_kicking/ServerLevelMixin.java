package dev.ryanhcode.sable.mixin.entity.entity_kicking;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Kicks entities out of sublevels if they are added to a {@link dev.ryanhcode.sable.sublevel.plot.LevelPlot}
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    public void sable$kickEntity(final Entity entity, final CallbackInfoReturnable<Boolean> cir) {
        if (!EntitySubLevelUtil.shouldKick(entity)) {
            return;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(entity);
        if (subLevel != null) {
            EntitySubLevelUtil.kickEntity(subLevel, entity);
        }
    }
}
