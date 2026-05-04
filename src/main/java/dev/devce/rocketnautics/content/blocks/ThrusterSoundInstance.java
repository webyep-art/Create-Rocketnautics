package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.registry.RocketSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThrusterSoundInstance extends AbstractTickableSoundInstance {
    private final IThruster blockEntity;

    public ThrusterSoundInstance(IThruster blockEntity) {
        super(RocketSounds.ROCKET_THRUST.get(), SoundSource.BLOCKS, blockEntity.getLevel().getRandom());
        this.blockEntity = blockEntity;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1f;
        this.pitch = 1.0f;
        this.x = blockEntity.getBlockPos().getX() + 0.5f;
        this.y = blockEntity.getBlockPos().getY() + 0.5f;
        this.z = blockEntity.getBlockPos().getZ() + 0.5f;
    }

    @Override
    public void tick() {
        if (blockEntity.isRemoved() || !blockEntity.isActive()) {
            this.stop();
            return;
        }

        com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour behaviour = blockEntity.getThrustPower();
        int power = (behaviour != null) ? behaviour.getValue() : 1;
        float targetVolume = 0.2f + (power / 10.0f);
        float targetPitch = 0.5f + (power / 20.0f);

        
        this.volume = Mth.lerp(0.1f, this.volume, targetVolume);
        this.pitch = Mth.lerp(0.1f, this.pitch, targetPitch);
        
        
        this.x = blockEntity.getBlockPos().getX() + 0.5f;
        this.y = blockEntity.getBlockPos().getY() + 0.5f;
        this.z = blockEntity.getBlockPos().getZ() + 0.5f;
    }

    public void stopSound() {
        this.stop();
    }
}
