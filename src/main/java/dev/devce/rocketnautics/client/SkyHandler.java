package dev.devce.rocketnautics.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.network.PlanetMapRequestPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SkyHandler {

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        if (y > 3000.0) {
            float factor = (float) Mth.clamp((y - 3000.0) / 2000.0, 0.0, 1.0);
            
            event.setRed(Mth.lerp(factor, event.getRed(), 0.0f));
            event.setGreen(Mth.lerp(factor, event.getGreen(), 0.0f));
            event.setBlue(Mth.lerp(factor, event.getBlue(), 0.0f));
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        if (camY < 2000.0) return;

        float visibility = (float) Mth.clamp((camY - 2000.0) / 2000.0, 0.0, 1.0);
        if (visibility <= 0) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        
        Camera camera = mc.gameRenderer.getMainCamera();

        Matrix4f matrix = poseStack.last().pose();
        matrix.identity();
        
        Quaternionf invRot = new Quaternionf(camera.rotation());
        invRot.conjugate();
        poseStack.mulPose(invRot);
        
        float renderDist = 20.0f;
        float parallaxFactor = (float) (renderDist / Math.max(100.0, camY)); 
        double camX = camera.getPosition().x;
        double camZ = camera.getPosition().z;
        float relX = (float) (-camX * parallaxFactor);
        float relY = -renderDist;
        float relZ = (float) (-camZ * parallaxFactor);
        
        float size = (float) (500000.0f * (renderDist / Math.max(100.0, camY)));
        
        ensurePlanetTexture();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        if (PLANET_TEXTURE_ID != null) {
            RenderSystem.setShaderTexture(0, PLANET_TEXTURE_ID);
        } else {
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "textures/environment/planet_map.png"));
        }
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        float r = 1.0f, g = 1.0f, b = 1.0f;
        
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        
        bufferbuilder.addVertex(matrix, relX - size, relY, relZ - size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);
        bufferbuilder.addVertex(matrix, relX - size, relY, relZ + size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);
        bufferbuilder.addVertex(matrix, relX + size, relY, relZ + size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, relX + size, relY, relZ - size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);

        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        
        ensureCloudTexture();
        if (CLOUD_TEXTURE_ID != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, CLOUD_TEXTURE_ID);
            
            float timeOffset = (System.currentTimeMillis() % 2000000L) / 100000.0f;
            
            BufferBuilder cloudBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            cloudBuilder.addVertex(matrix, relX - size, relY, relZ - size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f + timeOffset, 0.0f);
            cloudBuilder.addVertex(matrix, relX - size, relY, relZ + size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f + timeOffset, 1.0f);
            cloudBuilder.addVertex(matrix, relX + size, relY, relZ + size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f + timeOffset, 1.0f);
            cloudBuilder.addVertex(matrix, relX + size, relY, relZ - size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f + timeOffset, 0.0f);
            BufferUploader.drawWithShader(cloudBuilder.buildOrThrow());
        }

        ensureHaloTexture();
        if (HALO_TEXTURE_ID != null) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, HALO_TEXTURE_ID);
            
            float haloSize = size * 1.3f;
            BufferBuilder haloBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            haloBuilder.addVertex(matrix, relX - haloSize, relY, relZ - haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f, 0.0f);
            haloBuilder.addVertex(matrix, relX - haloSize, relY, relZ + haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f, 1.0f);
            haloBuilder.addVertex(matrix, relX + haloSize, relY, relZ + haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f, 1.0f);
            haloBuilder.addVertex(matrix, relX + haloSize, relY, relZ - haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f, 0.0f);
            BufferUploader.drawWithShader(haloBuilder.buildOrThrow());
            
            RenderSystem.defaultBlendFunc();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        if (y > 3000.0) {
            float factor = (float) Mth.clamp((y - 3000.0) / 2000.0, 0.0, 1.0);
            
            float start = event.getNearPlaneDistance();
            float end = event.getFarPlaneDistance();
            
            event.setNearPlaneDistance(Mth.lerp(factor, start, 1000.0f));
            event.setFarPlaneDistance(Mth.lerp(factor, end, 2000.0f));
            event.setCanceled(true);
        }
    }

    private static ResourceLocation PLANET_TEXTURE_ID = null;
    private static DynamicTexture PLANET_TEXTURE_OBJ = null;
    private static boolean mapRequested = false;

    private static void ensurePlanetTexture() {
        if (PLANET_TEXTURE_ID != null) return;
        Minecraft mc = Minecraft.getInstance();
        
        int size = 1024;
        NativeImage image = new NativeImage(size, size, false);
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int color = (255 << 24) | (80 << 16) | (40 << 8) | 10;
                image.setPixelRGBA(x, y, color);
            }
        }
        
        PLANET_TEXTURE_OBJ = new DynamicTexture(image);
        PLANET_TEXTURE_ID = mc.getTextureManager().register("rocketnautics_planet", PLANET_TEXTURE_OBJ);
        PLANET_TEXTURE_OBJ.setFilter(true, false);

        
        if (!mapRequested) {
            PacketDistributor.sendToServer(new PlanetMapRequestPayload());
            mapRequested = true;
        }
    }

    public static void updatePlanetTexture(byte[] data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (PLANET_TEXTURE_OBJ == null) return;
            
            int texSize = 1024;
            int dataSize = 256;
            NativeImage image = new NativeImage(texSize, texSize, false);
            
            for (int x = 0; x < texSize; x++) {
                for (int y = 0; y < texSize; y++) {
                    double u = (x / (double)texSize) * dataSize;
                    double v = (y / (double)texSize) * dataSize;
                    
                    double nx = x * 0.05;
                    double ny = y * 0.05;
                    double warpX = (Math.sin(nx) + 0.5 * Math.sin(nx * 2.1)) * 1.5;
                    double warpY = (Math.cos(ny) + 0.5 * Math.cos(ny * 2.1)) * 1.5;
                    
                    int sampleX = (int) Math.round(u + warpX);
                    int sampleY = (int) Math.round(v + warpY);
                    
                    if (sampleX < 0) sampleX = 0;
                    if (sampleX >= dataSize) sampleX = dataSize - 1;
                    if (sampleY < 0) sampleY = 0;
                    if (sampleY >= dataSize) sampleY = dataSize - 1;
                    
                    byte colorIdx = data[sampleX + sampleY * dataSize];
                    int r = 30, g = 120, b = 40;
                    switch (colorIdx) {
                        case 0: r = 10; g = 40; b = 120; break;
                        case 1: r = 20; g = 80; b = 180; break;
                        case 2: r = 210; g = 190; b = 140; break;
                        case 3: r = 200; g = 180; b = 100; break;
                        case 4: r = 30; g = 120; b = 40; break;
                        case 5: r = 20; g = 90; b = 30; break;
                        case 6: r = 10; g = 70; b = 20; break;
                        case 7: r = 20; g = 60; b = 40; break;
                        case 8: r = 220; g = 220; b = 230; break;
                        case 9: r = 180; g = 80; b = 30; break;
                        case 10: r = 120; g = 120; b = 120; break;
                    }
                    
                    int color = (255 << 24) | (b << 16) | (g << 8) | r;
                    image.setPixelRGBA(x, y, color);
                }
            }
            
            PLANET_TEXTURE_OBJ.setPixels(image);
            PLANET_TEXTURE_OBJ.upload();
            PLANET_TEXTURE_OBJ.setFilter(false, false);
        });
    }        


    private static ResourceLocation CLOUD_TEXTURE_ID = null;
    private static DynamicTexture CLOUD_TEXTURE_OBJ = null;

    private static void ensureCloudTexture() {
        if (CLOUD_TEXTURE_ID != null) return;
        Minecraft mc = Minecraft.getInstance();
        
        int size = 64;
        NativeImage image = new NativeImage(size, size, false);
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double nx = (x / (double)size) * 12.0;
                double ny = (y / (double)size) * 12.0;
                
                double value = Math.sin(nx + 5.0) * Math.cos(ny + 5.0) 
                             + 0.5 * Math.sin(nx * 2.0) * Math.cos(ny * 2.0)
                             + 0.25 * Math.sin(nx * 4.0) * Math.cos(ny * 4.0);
                
                int a = 0;
                if (value > 0.3) {
                    a = 200;
                }
                
                int color = (a << 24) | (255 << 16) | (255 << 8) | 255;
                image.setPixelRGBA(x, y, color);
            }
        }
        
        CLOUD_TEXTURE_OBJ = new DynamicTexture(image);
        CLOUD_TEXTURE_ID = mc.getTextureManager().register("rocketnautics_clouds", CLOUD_TEXTURE_OBJ);
        CLOUD_TEXTURE_OBJ.setFilter(false, false);
    }

    private static ResourceLocation HALO_TEXTURE_ID = null;
    private static boolean haloV5 = false;

    private static void ensureHaloTexture() {
        if (HALO_TEXTURE_ID != null && haloV5) return;
        Minecraft mc = Minecraft.getInstance();
        int size = 256;
        NativeImage image = new NativeImage(size, size, false);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double dx = (x - size / 2.0) / (size / 2.0);
                double dy = (y - size / 2.0) / (size / 2.0);
                
                double dist = Math.max(Math.abs(dx), Math.abs(dy));
                
                int r = 0, g = 0, b = 0, a = 0;
                double planetRadius = 1.0 / 1.3;
                
                if (dist <= planetRadius) {
                    double normalizedDist = dist / planetRadius; 
                    double opticalDepth = Math.pow(normalizedDist, 3.0);
                    
                    r = 40; g = 120; b = 255;
                    r += (int) (opticalDepth * 215); 
                    g += (int) (opticalDepth * 135); 
                    b += (int) (opticalDepth * 0);   
                    
                    r = Math.min(255, r); g = Math.min(255, g); b = Math.min(255, b);
                    a = 80 + (int) (opticalDepth * 175);
                } else if (dist <= 1.0) {
                    double gradient = (dist - planetRadius) / (1.0 - planetRadius);
                    double fade = Math.pow(1.0 - gradient, 1.0);
                    
                    r = (int) (140 * fade);
                    g = (int) (220 * fade);
                    b = (int) (255 * fade);
                    
                    a = (int) (255 * fade);
                }
                
                int color = (a << 24) | (b << 16) | (g << 8) | r;
                image.setPixelRGBA(x, y, color);
            }
        }
        DynamicTexture dynamicTexture = new DynamicTexture(image);
        HALO_TEXTURE_ID = mc.getTextureManager().register("rocketnautics_halo_v5", dynamicTexture);
        haloV5 = true;
    }
}
