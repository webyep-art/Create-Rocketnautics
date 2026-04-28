package dev.ryanhcode.sable.util;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Handles loading schematics
 */
public class SchematicLoader {

    public static @Nullable StructureTemplate loadSchematic(final ServerLevel level, final ResourceLocation location) {
        final String namespace = location.getNamespace();
        final String path = "schematics/" + location.getPath() + ".nbt";
        final ResourceLocation location1 = ResourceLocation.fromNamespaceAndPath(namespace, path);

        final Optional<Resource> option = level.getServer().getResourceManager().getResource(location1);
        if (option.isEmpty()) {
            return null;
        }

        final Resource resource = option.get();

        try (final InputStream stream = resource.open()) {
            final StructureTemplate template = new StructureTemplate();
            final CompoundTag nbt = NbtIo.readCompressed(stream, NbtAccounter.create(0x20000000L));
            template.load(level.holderLookup(Registries.BLOCK), nbt);
            return template;
        } catch (final IOException e) {
            return null;
        }
    }

    public static CompletableFuture<Set<ResourceLocation>> getSchematics(final MinecraftServer server) {
        return CompletableFuture.supplyAsync(() -> server.getResourceManager().listResources("schematics", path -> path.getPath().endsWith(".nbt")).keySet(), Util.backgroundExecutor());
    }

}
