package dev.ryanhcode.sable.api.schematic;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.netty.util.concurrent.FastThreadLocal;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

import java.util.Map;
import java.util.UUID;

/**
 * A global context for sub-levels being serialized/unserialized to/from schematics.
 * Block-entities and pieces of content that rely on sub-level dependencies are encouraged
 * to serialize differently using this context when being saved to schematics.
 */
public class SubLevelSchematicSerializationContext {
    private static final FastThreadLocal<SubLevelSchematicSerializationContext> THREAD_LOCAL = new FastThreadLocal<>();
    private final Map<UUID, SchematicMapping> mappings = new Object2ObjectOpenHashMap<>();
    private Function<BlockPos, BlockPos> placeTransform;
    private Function<BlockPos, BlockPos> setupTransform;
    private final Type type;
    private final BoundingBox3i boundingBox;

    public SubLevelSchematicSerializationContext(final Type type, final BoundingBox3i boundingBox) {
        this.type = type;
        this.boundingBox = boundingBox;
    }

    public Type getType() {
        return this.type;
    }

    public BoundingBox3i getBoundingBox() {
        return this.boundingBox;
    }

    public static SubLevelSchematicSerializationContext getCurrentContext() {
        return THREAD_LOCAL.get();
    }

    @ApiStatus.Internal
    public static void setCurrentContext(@Nullable final SubLevelSchematicSerializationContext context) {
        THREAD_LOCAL.set(context);
    }

    public Function<BlockPos, BlockPos> getPlaceTransform() {
        return this.placeTransform;
    }

    public Function<BlockPos, BlockPos> getSetupTransform() {
        return this.setupTransform;
    }

    @ApiStatus.Internal
    public void setPlaceTransform(final Function<BlockPos, BlockPos> transform) {
        this.placeTransform = transform;
    }


    @ApiStatus.Internal
    public void setSetupTransform(final Function<BlockPos, BlockPos> transform) {
        this.setupTransform = transform;
    }

    @Nullable
    public SchematicMapping getMapping(final SubLevel subLevel) {
        return this.mappings.get(subLevel.getUniqueId());
    }

    @Nullable
    public SchematicMapping getMapping(final UUID uuid) {
        return this.mappings.get(uuid);
    }

    @ApiStatus.Internal
    public Map<UUID, SchematicMapping> getMappings() {
        return this.mappings;
    }

    public record SchematicMapping(Vector3dc newCorner, Quaterniondc newOrientation, UUID newUUID,
                                   Function<BlockPos, BlockPos> transform) {
    }

    public enum Type {
        PLACE,
        SAVE
    }

}
