package dev.ryanhcode.sable.neoforge.mixin.compatibility.pmweather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.weather.WindEngine;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Add PMWeather wind to sable
 */
@Mixin(PMWeather.class)
public class PMWeatherMixin {

    @Inject(method = "<init>",  at = @At("TAIL"))
    private void init(final FMLModContainer container, final IEventBus bus, final Dist dist, final CallbackInfo ci) {
        SubLevelHelper.registerWindProvider((position, level) -> {
            return JOMLConversion.toJOML(WindEngine.getWind(JOMLConversion.toMojang(position), level));
        });
    }
}
