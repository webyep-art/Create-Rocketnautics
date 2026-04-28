package dev.ryanhcode.sable.physics.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class FloatingBlockMaterialDataHandler {
    public static HashMap<ResourceLocation, FloatingBlockMaterial> allMaterials = new HashMap<>();

    public static void addMaterial(final ResourceLocation id, final FloatingBlockMaterial material) {
        allMaterials.put(id, material);
    }

    public static void clearMaterials() {
        allMaterials.clear();
    }

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        public static final String NAME = "floating_block_material";
        public static final ResourceLocation ID = Sable.sablePath(NAME);

        private static final Gson GSON = new Gson();

        public static final ReloadListener INSTANCE = new ReloadListener();

        protected ReloadListener() {
            super(ReloadListener.GSON, "floating_materials");
        }

        @Override
        protected void apply(final Map<ResourceLocation, JsonElement> map, final ResourceManager resourceManager, final ProfilerFiller profiler) {
            FloatingBlockMaterialDataHandler.allMaterials.clear();
            for (final Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                final JsonElement element = entry.getValue();
                try {
                    final DataResult<FloatingBlockMaterial> dataResult = FloatingBlockMaterial.CODEC.parse(JsonOps.INSTANCE, element);

                    if (dataResult.error().isPresent()) {
                        Sable.LOGGER.error(String.valueOf(dataResult.error().get()));
                    } else {
                        final ResourceLocation loc = entry.getKey();
                        final FloatingBlockMaterial floatingBlockMaterial = dataResult.result().orElseThrow();

                        FloatingBlockMaterialDataHandler.addMaterial(loc, floatingBlockMaterial);
                    }
                } catch (final Exception ignored) {

                }
            }
        }
    }
}
