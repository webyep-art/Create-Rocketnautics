package dev.devce.rocketnautics.mixin;

import dev.devce.rocketnautics.RocketNauticsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void rocketnautics$onSetScreen(Screen screen, CallbackInfo ci) {
        // Если счетчик тиков бесшовного перехода еще не истек
        if (RocketNauticsClient.seamlessTransitionTicks > 0) {
            // Если игра пытается показать экран загрузки ("Receiving Level" или "Loading Terrain")
            if (screen instanceof ReceivingLevelScreen) {
                // ОТМЕНЯЕМ установку этого экрана. Игрок продолжит видеть мир.
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rocketnautics$onTick(CallbackInfo ci) {
        // Уменьшаем счетчик тиков бесшовности в каждом кадре
        if (RocketNauticsClient.seamlessTransitionTicks > 0) {
            RocketNauticsClient.seamlessTransitionTicks--;
        }
    }
}
