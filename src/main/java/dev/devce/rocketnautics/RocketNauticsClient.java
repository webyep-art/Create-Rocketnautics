package dev.devce.rocketnautics;

import net.minecraft.client.DeltaTracker;

import dev.devce.rocketnautics.content.blocks.VectorThrusterRenderer;
import dev.devce.rocketnautics.content.particles.RocketExhaustParticle;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketParticles;
import dev.devce.rocketnautics.registry.RocketPartials;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles client-side initialization, rendering, and UI.
 */
@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RocketNauticsClient {

    private static final int MAX_DEBUG_LOGS = 15;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND_COLOR = 0x88000000;

    public static int originalRenderDistance = -1;
    public static int seamlessTransitionTicks = 0;
    public static boolean showDebugOverlay = true;
    
    private static final List<LogEntry> debugLogs = Collections.synchronizedList(new ArrayList<>());

    public record LogEntry(String message, int color, long timestamp) {}

    public static void addLog(String message, int color) {
        debugLogs.add(new LogEntry(message, color, System.currentTimeMillis()));
        if (debugLogs.size() > MAX_DEBUG_LOGS) {
            debugLogs.remove(0);
        }
        RocketNautics.LOGGER.info("[RENDER INFO] {}", message);
    }

    public static void startSeamlessTransition() {
        Minecraft mc = Minecraft.getInstance();
        if (originalRenderDistance == -1) {
            originalRenderDistance = mc.options.renderDistance().get();
        }
        mc.options.renderDistance().set(2);
        seamlessTransitionTicks = 100; // Time for world loading
    }

    public static void endSeamlessTransition() {
        seamlessTransitionTicks = 0;
    }

    public static void stepUpRenderDistance() {
        Minecraft mc = Minecraft.getInstance();
        if (originalRenderDistance == -1) return;
        
        int current = mc.options.renderDistance().get();
        if (current < originalRenderDistance) {
            mc.options.renderDistance().set(Math.min(current + 2, originalRenderDistance));
        } else {
            originalRenderDistance = -1; // Recovery complete
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "transition_overlay"), 
                RocketNauticsClient::renderDebugOverlays);
    }

    private static void renderDebugOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !showDebugOverlay) return;

        // Render real-time debug info on the left
        if (mc.player != null && mc.level != null) {
            renderLeftDebugPanel(guiGraphics, mc);
        }

        // Render scrolling log on the right
        renderRightLogPanel(guiGraphics, mc);
    }

    private static void renderLeftDebugPanel(GuiGraphics guiGraphics, Minecraft mc) {
        int y = 5;
        String dimension = mc.level.dimension().location().getPath();
        double altitude = mc.player.getY();
        int renderDist = mc.options.renderDistance().get();

        drawDebugLine(guiGraphics, "--- [RocketNautics Debug] ---", 5, y, 0xFFAA00);
        y += LINE_HEIGHT;
        drawDebugLine(guiGraphics, "Dimension: " + dimension, 5, y, 0x55FFFF);
        y += LINE_HEIGHT;
        drawDebugLine(guiGraphics, String.format("Altitude: %.2f", altitude), 5, y, 0x55FFFF);
        y += LINE_HEIGHT;
        drawDebugLine(guiGraphics, "Render Distance: " + renderDist, 5, y, 0x55FF55);
        y += LINE_HEIGHT;
        drawDebugLine(guiGraphics, "Transition Ticks: " + seamlessTransitionTicks, 5, y, 0xFF5555);
    }

    private static void renderRightLogPanel(GuiGraphics guiGraphics, Minecraft mc) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int y = 5;
        
        synchronized (debugLogs) {
            for (int i = debugLogs.size() - 1; i >= 0; i--) {
                LogEntry entry = debugLogs.get(i);
                drawTextRight(guiGraphics, entry.message, screenWidth - 5, y, entry.color);
                y += LINE_HEIGHT;
            }
        }
    }

    private static void drawDebugLine(GuiGraphics gui, String text, int x, int y, int color) {
        int width = Minecraft.getInstance().font.width(text);
        gui.fill(x - 2, y - 1, x + width + 2, y + 9, BACKGROUND_COLOR);
        gui.drawString(Minecraft.getInstance().font, text, x, y, color, true);
    }

    private static void drawTextRight(GuiGraphics gui, String text, int x, int y, int color) {
        int width = Minecraft.getInstance().font.width(text);
        gui.fill(x - width - 2, y - 1, x + 2, y + 9, BACKGROUND_COLOR);
        gui.drawString(Minecraft.getInstance().font, text, x - width, y, color, true);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "space"), 
            new DimensionSpecialEffects(Float.NaN, false, DimensionSpecialEffects.SkyType.NORMAL, false, false) {
                @Override
                public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
                    return fogColor;
                }

                @Override
                public boolean isFoggyAt(int x, int y) {
                    return false;
                }
            });
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RocketParticles.PLASMA.get(), RocketExhaustParticle.FlameProvider::new);
        event.registerSpriteSet(RocketParticles.PLUME.get(), RocketExhaustParticle.FlameProvider::new);
        event.registerSpriteSet(RocketParticles.JET_SMOKE.get(), RocketExhaustParticle.SmokeProvider::new);
        event.registerSpriteSet(RocketParticles.BLUE_FLAME.get(), RocketExhaustParticle.FlameProvider::new);
        event.registerSpriteSet(RocketParticles.RCS_GAS.get(), RocketExhaustParticle.RCSGasProvider::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RocketPartials.init();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RocketBlockEntities.VECTOR_THRUSTER.get(), VectorThrusterRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(RocketPartials.VECTOR_THRUSTER_NOZZLE_MODEL);
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.BakingCompleted event) {
        RocketPartials.vectorThrusterNozzle = event.getModels().get(RocketPartials.VECTOR_THRUSTER_NOZZLE_MODEL);
    }
}
