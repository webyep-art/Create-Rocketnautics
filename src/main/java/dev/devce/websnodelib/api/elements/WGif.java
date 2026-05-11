package dev.devce.websnodelib.api.elements;

import dev.devce.websnodelib.api.WElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WGif extends WElement {
    private final List<ResourceLocation> frames = new ArrayList<>();
    private final List<Integer> delays = new ArrayList<>();
    private int totalFrames = 0;
    private long startTime = -1;
    private int totalDuration = 0;

    public WGif(ResourceLocation resource, int width, int height) {
        this.width = width;
        this.height = height;
        loadGif(resource);
    }

    private void loadGif(ResourceLocation resource) {
        try {
            var resourceOptional = Minecraft.getInstance().getResourceManager().getResource(resource);
            if (resourceOptional.isEmpty()) return;
            java.io.InputStream is = resourceOptional.get().open();
            ImageInputStream stream = ImageIO.createImageInputStream(is);
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(stream);

            int count = reader.getNumImages(true);
            for (int i = 0; i < count; i++) {
                BufferedImage bImg = reader.read(i);
                NativeImage nImg = new NativeImage(bImg.getWidth(), bImg.getHeight(), false);
                for (int y = 0; y < bImg.getHeight(); y++) {
                    for (int x = 0; x < bImg.getWidth(); x++) {
                        int argb = bImg.getRGB(x, y);
                        // Convert ARGB to ABGR for NativeImage
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        nImg.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }

                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("websnodelib", "gif_frame_" + System.nanoTime() + "_" + i);
                Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(nImg));
                frames.add(loc);
                
                // Get delay (default to 100ms if unknown)
                delays.add(100); 
                totalDuration += 100;
            }
            totalFrames = count;
            reader.dispose();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        if (frames.isEmpty()) return;

        if (startTime == -1) startTime = System.currentTimeMillis();
        long elapsed = (System.currentTimeMillis() - startTime) % Math.max(1, totalDuration);
        
        int currentFrame = 0;
        long currentTotal = 0;
        for (int i = 0; i < frames.size(); i++) {
            currentTotal += delays.get(i);
            if (elapsed < currentTotal) {
                currentFrame = i;
                break;
            }
        }

        graphics.blit(frames.get(currentFrame), x, y, 0, 0, width, height, width, height);
    }
}
