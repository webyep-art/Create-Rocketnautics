package dev.devce.rocketnautics.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DeepSpaceTexture extends DynamicTexture {
    private static final int TEX_SIZE = 1024;

    private static DeepSpaceTexture INSTANCE;
    private static ResourceLocation ID;

    private static final int[] warpSampling = new int[TEX_SIZE * TEX_SIZE];

    static {
        int dataSize = 256;
        for (int x = 0; x < TEX_SIZE; x++) {
            for (int y = 0; y < TEX_SIZE; y++) {
                double u = (x / (double)TEX_SIZE) * dataSize;
                double v = (y / (double)TEX_SIZE) * dataSize;

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

                warpSampling[x + y * TEX_SIZE] = sampleX + sampleY * dataSize;
            }
        }
    }

    public static DeepSpaceTexture getInstance() {
        if (INSTANCE == null) {
            Minecraft mc = Minecraft.getInstance();

            NativeImage image = new NativeImage(TEX_SIZE, TEX_SIZE, false);

            for (int x = 0; x < TEX_SIZE; x++) {
                for (int y = 0; y < TEX_SIZE; y++) {
                    image.setPixelRGBA(x, y, 0);
                }
            }

            INSTANCE = new DeepSpaceTexture(image);
            ID = mc.getTextureManager().register("rocketnautics_deep_space_planet", INSTANCE);
            INSTANCE.setFilter(true, false);
        }
        return INSTANCE;
    }

    public static ResourceLocation getInstanceID() {
        getInstance(); // ensure the instance is loaded
        return ID;
    }

    private DeepSpaceTexture(NativeImage p_117984_) {
        super(p_117984_);
    }

    public void loadData(byte[] data) {
        for (int x = 0; x < TEX_SIZE; x++) {
            for (int y = 0; y < TEX_SIZE; y++) {
                getPixels().setPixelRGBA(x, y, PlanetColors.getPackedColor(data[warpSampling[x + y * TEX_SIZE]]));
            }
        }

        upload();
        setFilter(false, false);
    }
}
