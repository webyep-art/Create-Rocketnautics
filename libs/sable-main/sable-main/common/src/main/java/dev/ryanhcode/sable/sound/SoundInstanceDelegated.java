package dev.ryanhcode.sable.sound;

/**
 * Stores the parent delegate for a delegated {@link net.minecraft.client.resources.sounds.SoundInstance}
 */
public interface SoundInstanceDelegated {

    MovingSoundInstanceDelegate getDelegate();

    void setDelegate(MovingSoundInstanceDelegate delegate);

}
