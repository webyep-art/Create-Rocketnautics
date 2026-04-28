package dev.ryanhcode.sable.sublevel.render.fancy;

import dev.ryanhcode.sable.Sable;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeQualifier;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.variable.GlslNewFieldNode;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Objects;

public class FancySubLevelShaderProcessor implements ShaderPreProcessor {

    public static final String BUFFER_SIZE = "SABLE_TEXTURE_CACHE_SIZE";

    @Override
    public void modify(final Context ctx, final GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        if (!(ctx instanceof final VeilContext veilContext) || !veilContext.isDynamic()) {
            return;
        }

        final ResourceLocation name = Objects.requireNonNull(veilContext.name(), "name");
        if (!name.getNamespace().equals(Sable.MOD_ID) || !name.getPath().startsWith("dynamic_sublevel/")) {
            return;
        }

        if (ctx.isVertex()) {
            veilContext.addDefinitionDependency(BUFFER_SIZE);

            // Remove all inputs
            tree.getBody().removeIf(next -> next instanceof final GlslNewFieldNode field && field.getType().getQualifiers().contains(GlslTypeQualifier.StorageType.IN));
            ctx.include(tree, Sable.sablePath("fancy_sublevel_vertex"), IncludeOverloadStrategy.FAIL);

            final GlslNodeList body = Objects.requireNonNull(tree.mainFunction().orElseThrow().getBody());
            body.add(0, GlslParser.parseExpression("_sable_unpack()"));
        }
    }
}
