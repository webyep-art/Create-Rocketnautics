package dev.devce.rocketnautics.mixin;

import dev.devce.rocketnautics.RocketNauticsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Redirect(
        method = "handleRespawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
        )
    )
    private void rocketnautics$redirectSetScreen(Minecraft instance, Screen screen) {
        // Используем обновленный счетчик тиков
        if (RocketNauticsClient.seamlessTransitionTicks > 0 && screen instanceof ReceivingLevelScreen) {
            return;
        }
        
        instance.setScreen(screen);
    }
}
