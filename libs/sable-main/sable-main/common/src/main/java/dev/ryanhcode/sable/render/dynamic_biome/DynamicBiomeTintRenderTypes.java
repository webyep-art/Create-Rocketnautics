package dev.ryanhcode.sable.render.dynamic_biome;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

public class DynamicBiomeTintRenderTypes extends RenderType {
    private static final String NAME = "dynamic_biome_tint";

    public DynamicBiomeTintRenderTypes(final String string, final VertexFormat vertexFormat, final VertexFormat.Mode mode, final int i, final boolean bl, final boolean bl2, final Runnable runnable, final Runnable runnable2) {
        super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
    }
//
//    public static void hello() {
//        RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
//                .setShaderState(VeilRenderBridge.shaderState(Sable.path("hello_there")))
//                .setTextureState(BLOCK_SHEET_MIPPED)
//                .setLightmapState(LIGHTMAP)
//                .createCompositeState(true);
//
//        RenderType.create(NAME,DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, RenderType.BIG_BUFFER_SIZE, true, false, rendertype$state);
//
//    }
}
