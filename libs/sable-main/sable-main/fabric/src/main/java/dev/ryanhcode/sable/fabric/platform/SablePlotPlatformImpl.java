package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.platform.SablePlotPlatform;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;

@SuppressWarnings("UnstableApiUsage")
public class SablePlotPlatformImpl implements SablePlotPlatform {

    @Override
    public void readLightData(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        // no-op
    }

    @Override
    public void readChunkAttachments(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        ((AttachmentTargetImpl) chunk).fabric_readAttachmentsFromNbt(tag, registryAccess);
    }

    @Override
    public void postLoad(final CompoundTag tag, final LevelChunk chunk) {
        // no-op
    }

    @Override
    public void writeLightData(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        // no-op
    }

    @Override
    public void writeChunkAttachments(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        ((AttachmentTargetImpl) chunk).fabric_writeAttachmentsToNbt(tag, registryAccess);
    }
}
