package dev.devce.rocketnautics.content.blocks.nodes.handlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import java.util.List;

public class AttitudeNodeHandler implements NodeHandler {

    @Override
    public Component getDisplayName() { return Component.literal("Attitude Indicator"); }

    @Override
    public String getCategory() { return "Display"; }

    @Override
    public Component getDescription() {
        return Component.literal("Visualizes ship orientation in 3D.\n\n" +
            "Input 1: Pitch\n" +
            "Input 2: Yaw\n" +
            "Input 3: Roll");
    }

    @Override
    public int getInputCount() { return 3; }

    @Override
    public List<Component> getInputNames() {
        return List.of(Component.literal("P"), Component.literal("Y"), Component.literal("R"));
    }

    @Override
    public double evaluate(Node node, NodeContext context) {
        node.lastPitch = context.evaluateInput(node.id, 0);
        node.lastYaw   = context.evaluateInput(node.id, 1);
        node.lastRoll  = context.evaluateInput(node.id, 2);
        return 0;
    }

    @Override
    public int renderCustomUI(GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        int padding = 4;
        int frameX = x + padding;
        int frameY = y + 5;
        int frameW = width - padding * 2;
        int frameH = 85; 
        
        int cx = frameX + frameW / 2;
        int cy = frameY + frameH / 2;

        // --- 1. Background ---
        graphics.fill(frameX, frameY, frameX + frameW, frameY + frameH, 0xFF0A0A14);
        graphics.renderOutline(frameX, frameY, frameW, frameH, 0xFF5555FF);

        // --- 2. Hardware 3D Rendering ---
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 150); 
        
        pose.mulPose(Axis.XP.rotationDegrees(20));
        pose.mulPose(Axis.YP.rotationDegrees(25));
        
        // --- 3. Rotation ---
        pose.mulPose(Axis.XP.rotationDegrees((float) node.lastPitch));
        pose.mulPose(Axis.ZP.rotationDegrees((float) node.lastYaw));
        pose.mulPose(Axis.YP.rotationDegrees((float) node.lastRoll));

        drawTexturedRocket(pose);

        pose.popPose();
        RenderSystem.disableDepthTest();

        // --- 4. Info Overlay ---
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawString(font, String.format("P: %.1f", node.lastPitch), frameX + 4, frameY + 4, 0x66FFFFFF, false);

        return frameH + 10;
    }

    private void drawTexturedRocket(PoseStack pose) {
        float bSize = 12f;
        float fS = bSize * 0.55f;
        float fOff = bSize / 2f + fS / 2f;
        float yOff = bSize;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = pose.last().pose();

        // Sprites
        TextureAtlasSprite copper   = getSprite("copper_block");
        TextureAtlasSprite iron     = getSprite("iron_block");
        TextureAtlasSprite glass    = getSprite("glass");
        TextureAtlasSprite redstone = getSprite("redstone_block");

        // Body blocks
        addBlock(buffer, matrix, 0,  yOff, 0, bSize, 0xFFFFFFFF, copper);   // bottom
        addBlock(buffer, matrix, 0,  0,    0, bSize, 0xFFFFFFFF, iron);     // center
        addBlock(buffer, matrix, 0, -yOff, 0, bSize, 0xFFFFFFFF, glass);    // top

        // Fins
        addBlock(buffer, matrix,  fOff, yOff, 0,    fS, 0xFFFFFFFF, redstone);
        addBlock(buffer, matrix, -fOff, yOff, 0,    fS, 0xFFFFFFFF, redstone);
        addBlock(buffer, matrix, 0,     yOff, fOff, fS, 0xFFFFFFFF, redstone);
        addBlock(buffer, matrix, 0,     yOff, -fOff, fS, 0xFFFFFFFF, redstone);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private TextureAtlasSprite getSprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.withDefaultNamespace("block/" + name));
    }

    private void addBlock(BufferBuilder buffer, Matrix4f mat, float x, float y, float z, float size, int color, TextureAtlasSprite sprite) {
        float hs = size / 2f;
        float x0=x-hs, x1=x+hs, y0=y-hs, y1=y+hs, z0=z-hs, z1=z+hs;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        // Top
        int c = shadeColor(color, 1.0f);
        buffer.addVertex(mat, x0, y0, z0).setUv(u0, v0).setColor(c);
        buffer.addVertex(mat, x0, y0, z1).setUv(u0, v1).setColor(c);
        buffer.addVertex(mat, x1, y0, z1).setUv(u1, v1).setColor(c);
        buffer.addVertex(mat, x1, y0, z0).setUv(u1, v0).setColor(c);
        
        // Bottom
        c = shadeColor(color, 0.5f);
        buffer.addVertex(mat, x0, y1, z0).setUv(u0, v0).setColor(c);
        buffer.addVertex(mat, x1, y1, z0).setUv(u1, v0).setColor(c);
        buffer.addVertex(mat, x1, y1, z1).setUv(u1, v1).setColor(c);
        buffer.addVertex(mat, x0, y1, z1).setUv(u0, v1).setColor(c);

        // Front (+Z)
        c = shadeColor(color, 0.8f);
        buffer.addVertex(mat, x0, y0, z1).setUv(u0, v0).setColor(c);
        buffer.addVertex(mat, x0, y1, z1).setUv(u0, v1).setColor(c);
        buffer.addVertex(mat, x1, y1, z1).setUv(u1, v1).setColor(c);
        buffer.addVertex(mat, x1, y0, z1).setUv(u1, v0).setColor(c);

        // Back (-Z)
        c = shadeColor(color, 0.8f);
        buffer.addVertex(mat, x0, y0, z0).setUv(u0, v0).setColor(c);
        buffer.addVertex(mat, x1, y0, z0).setUv(u1, v0).setColor(c);
        buffer.addVertex(mat, x1, y1, z0).setUv(u1, v1).setColor(c);
        buffer.addVertex(mat, x0, y1, z0).setUv(u0, v1).setColor(c);

        // Right (+X)
        c = shadeColor(color, 0.6f);
        buffer.addVertex(mat, x1, y0, z0).setUv(u0, v0).setColor(c);
        buffer.addVertex(mat, x1, y0, z1).setUv(u0, v1).setColor(c);
        buffer.addVertex(mat, x1, y1, z1).setUv(u1, v1).setColor(c);
        buffer.addVertex(mat, x1, y1, z0).setUv(u1, v0).setColor(c);

        // Left (-X)
        c = shadeColor(color, 0.6f);
        buffer.addVertex(mat, x0, y0, z0).setUv(u0, v0).setColor(c);
        buffer.addVertex(mat, x0, y1, z0).setUv(u0, v1).setColor(c);
        buffer.addVertex(mat, x0, y1, z1).setUv(u1, v1).setColor(c);
        buffer.addVertex(mat, x0, y0, z1).setUv(u1, v0).setColor(c);
    }

    private int shadeColor(int color, float light) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * light));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * light));
        int b = Math.min(255, (int) ((color & 0xFF) * light));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public String getIcon() { return "A"; }
    @Override
    public int getHeaderColor() { return 0xFF5555FF; }
}
