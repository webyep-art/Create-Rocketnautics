package dev.devce.rocketnautics.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import java.util.Random;

/**
 * Utility class for handling camera shake effects on the client.
 * Provides methods to add intensity and apply it to camera angles during rendering.
 */
public class CameraShakeHandler {
    private static float shakeIntensity = 0f;
    private static final Random random = new Random();

    public static void addShake(float intensity) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
    }

    /**
     * Reduces shake intensity over time (decay).
     */
    public static void tick() {
        if (shakeIntensity > 0) {
            shakeIntensity *= 0.95f; // Exponential decay
            if (shakeIntensity < 0.001f) shakeIntensity = 0;
        }
    }

    public static float getShakeIntensity() {
        return shakeIntensity;
    }

    /**
     * Modifies the given camera angles (pitch, yaw, roll) by applying random offsets
     * based on the current shake intensity.
     */
    public static void applyShake(float partialTicks, float[] angles) {
        if (shakeIntensity <= 0) return;

        float currentShake = shakeIntensity; 
        
        // Apply random jitter to each axis
        angles[0] += (random.nextFloat() - 0.5f) * currentShake * 25f; // Pitch
        angles[1] += (random.nextFloat() - 0.5f) * currentShake * 25f; // Yaw
        angles[2] += (random.nextFloat() - 0.5f) * currentShake * 10f; // Roll
    }
}
