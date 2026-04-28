package dev.ryanhcode.sable.mixin.sublevel_sounds;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Channel.class)
public interface ChannelAccessor {

    @Accessor
    int getSource();
}
