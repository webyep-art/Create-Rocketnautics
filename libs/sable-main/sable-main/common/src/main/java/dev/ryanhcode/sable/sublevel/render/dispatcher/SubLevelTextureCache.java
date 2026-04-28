package dev.ryanhcode.sable.sublevel.render.dispatcher;

import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelShaderProcessor;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.block.DynamicShaderBlock;
import foundry.veil.api.client.render.shader.block.ShaderBlock;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.lwjgl.system.NativeResource;

import java.util.Arrays;
import java.util.Objects;

public class SubLevelTextureCache implements NativeResource {

    private static final int SPRITE_SIZE = 8 * Float.BYTES;
    private static final int DEFAULT_SPRITE_COUNT = 32;

    private final Object2IntMap<PackedTexture> textures = Object2IntMaps.synchronize(new Object2IntOpenHashMap<>());
    private final Object2IntMap<PackedTexture> newTextures = new Object2IntArrayMap<>();

    private DynamicShaderBlock<PackedTexture[]> textureBlock;

    public SubLevelTextureCache() {
        this.textureBlock = null;
        VeilRenderSystem.renderer().getShaderDefinitions().set(FancySubLevelShaderProcessor.BUFFER_SIZE, Integer.toString(DEFAULT_SPRITE_COUNT));
    }

    public int getTextureId(final BakedQuad quad) {
        final int[] vertices = quad.getVertices();
        final float u0 = Float.intBitsToFloat(vertices[4]);
        final float v0 = Float.intBitsToFloat(vertices[5]);
        final float u1 = Float.intBitsToFloat(vertices[8 + 4]);
        final float v1 = Float.intBitsToFloat(vertices[8 + 5]);
        final float u2 = Float.intBitsToFloat(vertices[16 + 4]);
        final float v2 = Float.intBitsToFloat(vertices[16 + 5]);
        final float u3 = Float.intBitsToFloat(vertices[24 + 4]);
        final float v3 = Float.intBitsToFloat(vertices[24 + 5]);
        return this.textures.computeIfAbsent(new PackedTexture(u0, v0, u1, v1, u2, v2, u3, v3), texture -> {
            final int textureId = this.textures.size();
            this.newTextures.put((PackedTexture) texture, textureId);
            return textureId;
        });
    }

    public void flush() {
        if (this.newTextures.isEmpty()) {
            return;
        }

        if (this.textureBlock == null) {
            this.textureBlock = ShaderBlock.dynamic(ShaderBlock.BufferBinding.UNIFORM, DEFAULT_SPRITE_COUNT * SPRITE_SIZE, (packedTextures, byteBuffer) -> {
                for (final PackedTexture texture : packedTextures) {
                    if (texture == null) {
                        break;
                    }
                    byteBuffer.putFloat(texture.u0);
                    byteBuffer.putFloat(texture.u1);
                    byteBuffer.putFloat(texture.u2);
                    byteBuffer.putFloat(texture.u3);
                    byteBuffer.putFloat(texture.v0);
                    byteBuffer.putFloat(texture.v1);
                    byteBuffer.putFloat(texture.v2);
                    byteBuffer.putFloat(texture.v3);
                }
            });
            this.textureBlock.set(new PackedTexture[DEFAULT_SPRITE_COUNT]);
        }

        final int expectedSize = this.textures.size() + this.newTextures.size();
        if (expectedSize * SPRITE_SIZE > this.textureBlock.getSize()) {
            final int newSize = (int) (expectedSize * 1.5);
            this.textureBlock.setSize(newSize * SPRITE_SIZE);

            final PackedTexture[] packedTextures = Objects.requireNonNull(this.textureBlock.getValue());
            this.textureBlock.set(Arrays.copyOf(packedTextures, newSize));
            VeilRenderSystem.renderer().getShaderDefinitions().set(FancySubLevelShaderProcessor.BUFFER_SIZE, Long.toString(newSize));
        }

        final PackedTexture[] packedTextures = Objects.requireNonNull(this.textureBlock.getValue());
        for (final Object2IntMap.Entry<PackedTexture> entry : this.newTextures.object2IntEntrySet()) {
            packedTextures[entry.getIntValue()] = entry.getKey();
        }
        this.newTextures.clear();
        this.textureBlock.set(packedTextures);
    }

    public void bind() {
        this.flush();
        if (this.textureBlock != null) {
            VeilRenderSystem.bind("SableSprites", this.textureBlock);
        }
    }

    @Override
    public void free() {
        if (this.textureBlock != null) {
            VeilRenderSystem.unbind(this.textureBlock);
            this.textureBlock.free();
            this.textureBlock = null;
        }
    }

    private record PackedTexture(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
    }
}
