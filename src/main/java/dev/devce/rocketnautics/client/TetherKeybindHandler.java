package dev.devce.rocketnautics.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.network.TetherDetachPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TetherKeybindHandler {

    private static final KeyMapping DETACH_TETHER = new KeyMapping(
            "key.rocketnautics.detach_tether",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.rocketnautics"
    );

    private TetherKeybindHandler() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DETACH_TETHER);
    }

    @EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class RuntimeEvents {
        private RuntimeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            while (DETACH_TETHER.consumeClick()) {
                PacketDistributor.sendToServer(new TetherDetachPayload());
            }
        }
    }
}
