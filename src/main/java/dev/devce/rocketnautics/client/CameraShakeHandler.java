package dev.devce.rocketnautics.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import java.util.Random;

public class CameraShakeHandler {
    private static float shakeIntensity = 0f;
    private static final Random random = new Random();

    public static void addShake(float intensity) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
    }

    public static void tick() {
        if (shakeIntensity > 0) {
            shakeIntensity *= 0.95f; 
            if (shakeIntensity < 0.001f) shakeIntensity = 0;
        }
    }

    public static float getShakeIntensity() {
        return shakeIntensity;
    }

    public static void applyShake(float partialTicks, float[] angles) {
        if (shakeIntensity <= 0) return;

        
        float currentShake = shakeIntensity; 
        
        
        angles[0] += (random.nextFloat() - 0.5f) * currentShake * 25f;
        angles[1] += (random.nextFloat() - 0.5f) * currentShake * 25f;
        angles[2] += (random.nextFloat() - 0.5f) * currentShake * 10f;
    }
}
