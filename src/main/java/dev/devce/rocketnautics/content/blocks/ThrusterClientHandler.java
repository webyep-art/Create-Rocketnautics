package dev.devce.rocketnautics.content.blocks;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThrusterClientHandler {
    public static void startSound(IThruster thruster, SoundContainer container) {
        Minecraft mc = Minecraft.getInstance();
        ThrusterSoundInstance instance = new ThrusterSoundInstance(thruster);
        mc.getSoundManager().play(instance);
        container.soundInstance = instance;
    }

    public static void stopSound(SoundContainer container) {
        if (container.soundInstance instanceof ThrusterSoundInstance instance) {
            instance.stopSound();
            container.soundInstance = null;
        }
    }

    
    public static class SoundContainer {
        public Object soundInstance;
    }
}
