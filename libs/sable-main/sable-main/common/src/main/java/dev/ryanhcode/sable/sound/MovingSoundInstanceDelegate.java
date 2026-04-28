package dev.ryanhcode.sable.sound;

import com.mojang.blaze3d.audio.Channel;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixin.sublevel_sounds.ChannelAccessor;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import static org.lwjgl.openal.AL11.*;

/**
 * A moving sound instance that delegates to another sound instance for the sound data
 */
public class MovingSoundInstanceDelegate implements SoundInstance, TickableSoundInstance {

    private SubLevel subLevel;
    private double latestX, latestY, latestZ;
    public SoundInstance instance;

    public MovingSoundInstanceDelegate(final SoundInstance instance, final SubLevel subLevel) {
        this.instance = instance;
        this.subLevel = subLevel;

        if (this.instance instanceof SoundInstanceDelegated) {
            ((SoundInstanceDelegated) this.instance).setDelegate(this);
        }
    }

    /**
     * Ticks and updates the velocity of the sound source
     *
     * @param channel The channel to update
     */
    public void tickWithChannel(final Channel channel) {
        final int source = ((ChannelAccessor) channel).getSource();

        if (this.subLevel != null && this.subLevel.isRemoved()) this.subLevel = null;
        if (this.subLevel == null) {
            alSource3f(source, AL_VELOCITY, 0, 0, 0);
            return;
        }

        final Vector3d instancePos = new Vector3d(this.instance.getX(), this.instance.getY(), this.instance.getZ());
        final Vector3d motion = Sable.HELPER.getVelocity(Minecraft.getInstance().level, instancePos);
        final Entity player = Minecraft.getInstance().getCameraEntity();

        if (player == null) {
            alSource3f(source, AL_VELOCITY, 0, 0, 0);
            return;
        }

        final Vector3d playerPosition = JOMLConversion.toJOML(player.position());
        final Vector3d playerMotion = playerPosition.sub(player.xo, player.yo, player.zo).mul(20.0); // 20 ticks per second

        alSpeedOfSound(1800.0F);
        alDopplerFactor(0.4F);

        alSource3f(source, AL_VELOCITY,
                (float) (motion.x - playerMotion.x),
                (float) (motion.y - playerMotion.y),
                (float) (motion.z - playerMotion.z));
    }

    public void unload(final Channel channel) {
        alSource3f(((ChannelAccessor) channel).getSource(), AL_VELOCITY, 0, 0, 0);
    }

    @Override
    public @NotNull ResourceLocation getLocation() {
        return this.instance.getLocation();
    }

    @Override
    public WeighedSoundEvents resolve(final SoundManager pManager) {
        return this.instance.resolve(pManager);
    }

    @Override
    public @NotNull Sound getSound() {
        return this.instance.getSound();
    }

    @Override
    public @NotNull SoundSource getSource() {
        return this.instance.getSource();
    }

    @Override
    public boolean isLooping() {
        return this.instance.isLooping();
    }

    @Override
    public boolean isRelative() {
        return this.instance.isRelative();
    }

    @Override
    public int getDelay() {
        return this.instance.getDelay();
    }

    @Override
    public float getVolume() {
        return this.instance.getVolume();
    }

    @Override
    public float getPitch() {
        return this.instance.getPitch();
    }

    @Override
    public double getX() {
        if (this.subLevel == null) return this.latestX;
        return this.latestX = this.subLevel.logicalPose().transformPosition(new Vec3(this.instance.getX(), this.instance.getY(), this.instance.getZ())).x;
    }

    @Override
    public double getY() {
        if (this.subLevel == null) return this.latestY;
        return this.latestY = this.subLevel.logicalPose().transformPosition(new Vec3(this.instance.getX(), this.instance.getY(), this.instance.getZ())).y;
    }

    @Override
    public double getZ() {
        if (this.subLevel == null) return this.latestZ;
        return this.latestZ = this.subLevel.logicalPose().transformPosition(new Vec3(this.instance.getX(), this.instance.getY(), this.instance.getZ())).z;
    }

    @Override
    public boolean canStartSilent() {
        return this.instance.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return this.instance.canPlaySound();
    }

    @Override
    public Attenuation getAttenuation() {
        return this.instance.getAttenuation();
    }

    @Override
    public boolean isStopped() {
        if (this.instance instanceof final TickableSoundInstance tickable) {
            return tickable.isStopped();
        }

        return !this.instance.canPlaySound();
    }

    @Override
    public void tick() {
        if (this.instance instanceof final TickableSoundInstance tickable) {
            tickable.tick();
        }
    }
}
