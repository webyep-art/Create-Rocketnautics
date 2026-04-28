package dev.ryanhcode.sable.neoforge.platform;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.platform.SablePlotPlatform;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import org.slf4j.Logger;

@SuppressWarnings("UnstableApiUsage")
public class SablePlotPlatformImpl implements SablePlotPlatform {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void readLightData(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        if (tag.contains(LevelChunkAuxiliaryLightManager.LIGHT_NBT_KEY, Tag.TAG_LIST)) {
            chunk.getAuxLightManager(chunk.getPos()).deserializeNBT(registryAccess, tag.getList(LevelChunkAuxiliaryLightManager.LIGHT_NBT_KEY, Tag.TAG_COMPOUND));
        }
    }

    @Override
    public void readChunkAttachments(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        if (tag.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY, Tag.TAG_COMPOUND)) {
            chunk.readAttachmentsFromNBT(registryAccess, tag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY));
        }
    }

    @Override
    public void postLoad(final CompoundTag tag, final LevelChunk chunk) {
        NeoForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk, tag, ChunkType.LEVELCHUNK));
    }

    @Override
    public void writeLightData(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        final Tag lightTag = chunk.getAuxLightManager(chunk.getPos()).serializeNBT(registryAccess);
        if (lightTag != null) {
            tag.put(LevelChunkAuxiliaryLightManager.LIGHT_NBT_KEY, lightTag);
        }
    }

    @Override
    public void writeChunkAttachments(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        try {
            final CompoundTag capTag = chunk.writeAttachmentsToNBT(registryAccess);
            if (capTag != null) {
                tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, capTag);
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to write chunk attachments. An attachment has likely thrown an exception trying to write state. It will not persist. Report this to the mod author", e);
        }
    }
}
