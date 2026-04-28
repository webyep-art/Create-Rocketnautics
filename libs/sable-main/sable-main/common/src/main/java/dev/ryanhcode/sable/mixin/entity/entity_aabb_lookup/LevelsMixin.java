package dev.ryanhcode.sable.mixin.entity.entity_aabb_lookup;

import dev.ryanhcode.sable.util.SubLevelInclusiveLevelEntityGetter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wraps the client and server level {@link net.minecraft.world.level.entity.LevelEntityGetterAdapter} in a {@link SubLevelInclusiveLevelEntityGetter}
 */
@Pseudo
@Mixin({ServerLevel.class, ClientLevel.class,})
public class LevelsMixin {

    @Inject(method = "getEntities()Lnet/minecraft/world/level/entity/LevelEntityGetter;", at = @At("RETURN"), cancellable = true)
    private void sable$postGetEntities(final CallbackInfoReturnable<LevelEntityGetter<Entity>> cir) {
        cir.setReturnValue(new SubLevelInclusiveLevelEntityGetter<>((Level) (Object) this, cir.getReturnValue()));
    }

}
