package dev.ryanhcode.sable.sublevel.render.dispatcher;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import foundry.veil.api.client.render.CullFrustum;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.lwjgl.system.NativeResource;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Renders sub-levels into the world.
 */
@ApiStatus.Internal
public interface SubLevelRenderDispatcher extends NativeResource, ResourceManagerReloadListener {

    /**
     * @return The current sub-level renderer instance
     */
    static SubLevelRenderDispatcher get() {
        return SubLevelRenderer.getDispatcher();
    }

    /**
     * Resizes the specified render data.
     *
     * @param subLevel   The sub-level to resize
     * @param renderData The current render data
     * @return The new render data to use
     */
    SubLevelRenderData resize(final ClientSubLevel subLevel, final SubLevelRenderData renderData);

    /**
     * Creates a new render data instance for the specified sub-level.
     *
     * @param subLevel The sub-level to create render data for
     * @return A new render data instance
     */
    SubLevelRenderData createRenderData(final ClientSubLevel subLevel);

    /**
     * Rebuilds the specified sub-levels when F3+A is pressed.
     *
     * @param sublevels The sub-levels to rebuild
     */
    default void rebuild(final Iterable<ClientSubLevel> sublevels) {
        for (final ClientSubLevel sublevel : sublevels) {
            sublevel.getRenderData().rebuild();
        }
    }

    /**
     * Updates the current culling state for all sub-levels.
     *
     * @param sublevels   The sub-levels to update
     * @param cameraX     The x position of the camera
     * @param cameraY     The y position of the camera
     * @param cameraZ     The z position of the camera
     * @param cullFrustum The current frustum used for culling
     * @param isSpectator Whether the player is in spectator mode
     */
    void updateCulling(final Iterable<ClientSubLevel> sublevels, final double cameraX, final double cameraY, final double cameraZ, final CullFrustum cullFrustum, boolean isSpectator);

    /**
     * Renders all sub-levels into the specified section layer.
     *
     * @param sublevels    The sub-levels to render
     * @param renderType   The render type being rendered
     * @param shader       The currently bound shader
     * @param cameraX      The x position of the camera
     * @param cameraY      The y position of the camera
     * @param cameraZ      The z position of the camera
     * @param modelView    The modelview matrix
     * @param projection   The projection matrix
     * @param partialTicks The percentage from last tick to this tick
     */
    void renderSectionLayer(final Iterable<ClientSubLevel> sublevels, final RenderType renderType, final ShaderInstance shader, final double cameraX, final double cameraY, final double cameraZ, final Matrix4f modelView, final Matrix4f projection, final float partialTicks);

    /**
     * Renders all sub-levels after the section layers have been rendered.
     *
     * @param sublevels    The sub-levels to render
     * @param cameraX      The x position of the camera
     * @param cameraY      The y position of the camera
     * @param cameraZ      The z position of the camera
     * @param modelView    The modelview matrix
     * @param projection   The projection matrix
     * @param partialTicks The percentage from last tick to this tick
     */
    void renderAfterSections(final Iterable<ClientSubLevel> sublevels, final double cameraX, double cameraY, double cameraZ, final Matrix4f modelView, final Matrix4f projection, final float partialTicks);

    void renderBlockEntities(final Iterable<ClientSubLevel> sublevels, final BlockEntityRenderer blockEntityRenderer, final double cameraX, double cameraY, double cameraZ, final float partialTick);

    void addDebugInfo(final Consumer<String> consumer);

    default void preRenderChunks(final Camera camera) {
    }

    interface BlockEntityRenderer {

        default void renderBlockEntities(final Collection<BlockEntity> blockEntities, final PoseStack poseStack, final float partialTick, final double cameraX, final double cameraY, final double cameraZ) {
            for (final BlockEntity blockEntity : blockEntities) {
                this.renderSingleBE(blockEntity, poseStack, partialTick, cameraX, cameraY, cameraZ);
            }
        }

        void renderSingleBE(final BlockEntity blockEntity, final PoseStack poseStack, final float partialTick, final double cameraX, final double cameraY, final double cameraZ);

        BlockEntityRenderDispatcher getBlockEntityRenderDispatcher();
    }
}
