package dev.ryanhcode.sable.render.sky_light_shadow;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslNode;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.expression.GlslAssignmentNode;
import io.github.ocelot.glslprocessor.api.node.expression.GlslOperationNode;
import io.github.ocelot.glslprocessor.api.node.function.GlslInvokeFunctionNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslVariableNode;
import net.minecraft.client.renderer.RenderType;

import java.util.List;

public class SableDynamicSkyLightShadowPreProcessor implements ShaderPreProcessor {
    public static final String SAMPLER_NAME = "SableShadowSampler";
    public static final String SHADOW_VOLUME_SIZE_UNIFORM = "SableShadowVolumeSize";
    public static final String ENABLE_UNIFORM = "SableShadowsEnabled";
    public static final String SHADOW_ORIGIN_UNIFORM = "SableShadowOrigin";

    @Override
    public void modify(final Context ctx, final GlslTree tree) throws GlslSyntaxException {
        if (!SableSkyLightShadows.isEnabled()) {
            return;
        }

        if (!ctx.isSourceFile()) {
            return;
        }

        if (ctx instanceof final MinecraftContext minecraftContext) {
            final List<RenderType> renderTypes = RenderType.chunkBufferLayers();

            boolean anyMatches = false;

            for (final RenderType renderType : renderTypes) {
                if (ctx.isVertex() && minecraftContext.shaderInstance().equals("rendertype_%s".formatted(renderType.name))) {
                    anyMatches = true;
                }
            }

            if (!anyMatches) {
                return;
            }
        } else {
            return;
        }

        final GlslNodeList mainFunctionBody = tree.mainFunction().orElseThrow().getBody();
        assert mainFunctionBody != null;

        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform sampler2D %s;".formatted(SAMPLER_NAME)));
        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float %s;".formatted(SHADOW_VOLUME_SIZE_UNIFORM)));
        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float %s;".formatted(ENABLE_UNIFORM)));
        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform vec3 %s;".formatted(SHADOW_ORIGIN_UNIFORM)));

        for (int i = 0; i < mainFunctionBody.size(); i++) {
            final GlslNode node = mainFunctionBody.get(i);

            if (node instanceof final GlslAssignmentNode assignmentNode && assignmentNode.getOperand() == GlslAssignmentNode.Operand.EQUAL) {
                // We can be for sure it's an assignment. Now let's check that it's calling
                // minecraft_sample_lightmap
                final GlslNode second = assignmentNode.getSecond();

                if (second instanceof final GlslOperationNode operationNode && operationNode.getOperand() == GlslOperationNode.Operand.MULTIPLY) {
//                        Sable.LOGGER.info("Found a multiply operation ", operationNode);

                    if (operationNode.getSecond() instanceof final GlslInvokeFunctionNode invokeNode && invokeNode.getHeader() instanceof final GlslVariableNode variableNode && variableNode.getName().equals("minecraft_sample_lightmap")) {
                        final List<GlslNode> replacementNodes = GlslParser.parseExpressionList("""
                                
                                                                    float skyLightScale;
                                                                    if (%s > 0.0) {
                                                                        float volumeSize = %s;
                                                                        vec3 shadowOrigin = %s;
                                                                        vec2 shadowUv = ((pos.xz - shadowOrigin.xz) * vec2(1.0, -1.0) + volumeSize) / (volumeSize * 2.0);
                                
                                                                        float sampleAverage = 0.0;                                        
                                                                        int sampleRadius = 3;
                                                                        float spacing = 1.0;
                                
                                                                        for (int i = -sampleRadius; i <= sampleRadius; i++) {
                                                                            for (int j = -sampleRadius; j <= sampleRadius; j++) {
                                                                                float depthSample = texture(%s, shadowUv + vec2(i, j) * spacing / (volumeSize * 2.0)).r;
                                
                                                                                // TODO: Pass shadow near plane in
                                                                                float depth = 0.5 + depthSample * (volumeSize - 0.5);
                                
                                                                                float y = shadowOrigin.y - depth;
                                
                                //                                                pos = Position + ChunkOffset;
                                //                                                pos.y = y;
                                //                                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                
                                                                                if (y >= pos.y) {
                                                                                    float strength = max(min((y - pos.y - 2.0) / 15.0, 1.0), 0.0);
                                                                                    float scale = (i + j) / float(sampleRadius);
                                                                                    sampleAverage += max(1.0 - scale, 0.0) * 0.6 * strength;
                                                                                } 
                                                                            }
                                                                        }
                                
                                                                        sampleAverage /= float((sampleRadius * 2 + 1) * (sampleRadius * 2 + 1));   
                                                                        skyLightScale = smoothstep(0.0, 1.0, 1.0 - sampleAverage);
                                                                    } else {
                                                                        skyLightScale = 1.0;
                                                                    }
                                
                                                                    vec2 sableLightModification = vec2(1.0, skyLightScale);
                                
                                                                    vertexColor = Color * minecraft_sample_lightmap(Sampler2, ivec2(UV2 * sableLightModification));
                                
                                """.formatted(ENABLE_UNIFORM, SHADOW_VOLUME_SIZE_UNIFORM, SHADOW_ORIGIN_UNIFORM, SAMPLER_NAME));

                        mainFunctionBody.set(i, replacementNodes.get(0));

                        for (int j = 1; j < replacementNodes.size(); j++) {
                            mainFunctionBody.add(i + j, replacementNodes.get(j));
                        }

                        break;
                    }
                }
            }
        }
    }
}
