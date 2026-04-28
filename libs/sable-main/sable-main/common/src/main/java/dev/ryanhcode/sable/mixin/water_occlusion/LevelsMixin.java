package dev.ryanhcode.sable.mixin.water_occlusion;

import dev.ryanhcode.sable.mixinterface.water_occlusion.WaterOcclusionContainerHolder;
import dev.ryanhcode.sable.platform.SablePlatform;
import dev.ryanhcode.sable.sublevel.water_occlusion.ClientWaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.ServerWaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a {@link WaterOcclusionContainer} to the server levels
 */
@Pseudo
@Mixin({ServerLevel.class, ClientLevel.class})
public class LevelsMixin implements WaterOcclusionContainerHolder {
    @Unique
    private final WaterOcclusionContainer<?> sable$waterOcclusionContainer = this.sable$createWaterOcclusionContainer();

    @Unique
    private WaterOcclusionContainer<?> sable$createWaterOcclusionContainer() {
        final Level self = (Level) (Object) this;
        if (SablePlatform.INSTANCE.isWrappedLevel(self)) return null;

        if (self.isClientSide) {
            return ClientWaterOcclusionContainer.create(self);
        } else {
            return ServerWaterOcclusionContainer.create(self);
        }
    }

    @Override
    public WaterOcclusionContainer<?> sable$getWaterOcclusionContainer() {
        return this.sable$waterOcclusionContainer;
    }
}
