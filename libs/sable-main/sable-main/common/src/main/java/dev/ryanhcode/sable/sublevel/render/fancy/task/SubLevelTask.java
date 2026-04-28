package dev.ryanhcode.sable.sublevel.render.fancy.task;

import dev.ryanhcode.sable.sublevel.render.fancy.BucketRenderBuffer;
import dev.ryanhcode.sable.sublevel.render.fancy.SubLevelMeshBuilder;
import net.minecraft.client.renderer.SectionBufferBuilderPack;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface SubLevelTask {

    void process(final SectionBufferBuilderPack pack, final MeshUploader uploader);

    interface MeshUploader {
        CompletableFuture<BucketRenderBuffer.Slice[]> upload(final SubLevelMeshBuilder.QuadMesh mesh);

        SubLevelMeshBuilder getMeshBuilder();
    }
}
