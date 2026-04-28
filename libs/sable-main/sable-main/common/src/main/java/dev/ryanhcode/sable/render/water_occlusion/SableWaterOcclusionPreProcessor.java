package dev.ryanhcode.sable.render.water_occlusion;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;

public class SableWaterOcclusionPreProcessor implements ShaderPreProcessor {
    public static final String CLOSE_SAMPLER_NAME = "SableCloseSampler";
    public static final String FAR_SAMPLER_NAME = "SableFarSampler";
    public static final String ENABLE_UNIFORM = "SableWaterOcclusionEnabled";

    @Override
    public void modify(final Context ctx, final GlslTree tree) throws GlslSyntaxException {
        if (!WaterOcclusionRenderer.isEnabled()) {
            return;
        }

        if (!ctx.isSourceFile()) {
            return;
        }

        if (!(ctx instanceof final MinecraftContext minecraftContext)) {
            return;
        }

        if (!ctx.isFragment() || !minecraftContext.shaderInstance().equals("rendertype_translucent")) {
            return;
        }

        final GlslNodeList mainFunctionBody = tree.mainFunction().orElseThrow().getBody();
        assert mainFunctionBody != null;

        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform vec2 %s;".formatted("ScreenSize")));
        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform sampler2D %s;".formatted(CLOSE_SAMPLER_NAME)));
        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform sampler2D %s;".formatted(FAR_SAMPLER_NAME)));
        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float %s;".formatted(ENABLE_UNIFORM)));

        mainFunctionBody.add(1, GlslParser.parseExpression("""
                if(%s > 0.0) {
                    float closeDepth = texture(%s, gl_FragCoord.xy / ScreenSize).r;
                    float farDepth = texture(%s, gl_FragCoord.xy / ScreenSize).r;
                    float waterDepth = gl_FragCoord.z;
                    if (waterDepth > closeDepth && waterDepth < farDepth) { discard; }
                }
                """.formatted(ENABLE_UNIFORM, CLOSE_SAMPLER_NAME, FAR_SAMPLER_NAME).trim()));
    }
}
