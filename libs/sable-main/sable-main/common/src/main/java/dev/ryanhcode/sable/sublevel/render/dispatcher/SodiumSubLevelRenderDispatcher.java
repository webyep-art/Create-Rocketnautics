package dev.ryanhcode.sable.sublevel.render.dispatcher;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.sodium.SodiumSubLevelRenderData;
import foundry.veil.api.client.render.CullFrustum;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;

import java.util.function.Consumer;

public class SodiumSubLevelRenderDispatcher implements SubLevelRenderDispatcher {

    @Override
    public SubLevelRenderData resize(final ClientSubLevel subLevel, final SubLevelRenderData renderData) {
        ((SodiumSubLevelRenderData) renderData).resize();
        return renderData;
    }


    @Override
    public SubLevelRenderData createRenderData(final ClientSubLevel subLevel) {
        return new SodiumSubLevelRenderData(subLevel);
    }


    @Override
    public void updateCulling(final Iterable<ClientSubLevel> sublevels, final double cameraX, final double cameraY, final double cameraZ, final CullFrustum cullFrustum, final boolean isSpectator) {

    }

    @Override
    public void renderSectionLayer(final Iterable<ClientSubLevel> sublevels, final RenderType renderType, final ShaderInstance shader, final double cameraX, final double cameraY, final double cameraZ, final Matrix4f modelView, final Matrix4f projection, final float partialTicks) {

    }

    @Override
    public void renderAfterSections(final Iterable<ClientSubLevel> sublevels, final double cameraX, final double cameraY, final double cameraZ, final Matrix4f modelView, final Matrix4f projection, final float partialTicks) {

    }

    @Override
    public void renderBlockEntities(final Iterable<ClientSubLevel> sublevels, final BlockEntityRenderer blockEntityRenderer, final double cameraX, final double cameraY, final double cameraZ, final float partialTick) {

    }

    @Override
    public void addDebugInfo(final Consumer<String> consumer) {

    }

    @Override
    public void onResourceManagerReload(final ResourceManager resourceManager) {

    }

    @Override
    public void free() {

    }
}
