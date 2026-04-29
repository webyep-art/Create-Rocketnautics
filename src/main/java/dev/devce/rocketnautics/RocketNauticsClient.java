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
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import org.joml.Vector3d;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles client-side initialization, rendering, and UI.
 */
@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RocketNauticsClient {

    private static final int MAX_DEBUG_LOGS = 15;
    private static final int LINE_HEIGHT = 12;
    private static final int BG_COLOR = 0xCC1A1A1A; // Flat Dark Gray
    private static final int ACCENT_GOLD = 0xFFFFD700; // Gold
    private static final int PANEL_BORDER = 0xEE333333;

    public static int originalRenderDistance = -1;
    public static int seamlessTransitionTicks = 0;
    public static boolean showDebugOverlay = false;
    
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

        renderBottomStatusBar(guiGraphics, mc);

        if (mc.player != null && mc.level != null) {
            renderLeftDebugPanel(guiGraphics, mc);
        }

        renderRightLogPanel(guiGraphics, mc);
    }

    private static void renderBottomStatusBar(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !mc.player.getGameProfile().getName().equals("Dev")) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int barHeight = 12;
        int y = height - 15;

        String text = "COSMONAUTICS | TEST BUILD";
        int textWidth = mc.font.width(text);
        int boxWidth = textWidth + 10;
        
        guiGraphics.fill(width - boxWidth - 5, y - 2, width - 5, y + barHeight, BG_COLOR);
        guiGraphics.fill(width - boxWidth - 5, y - 2, width - boxWidth - 3, y + barHeight, ACCENT_GOLD);
        guiGraphics.drawString(mc.font, text, width - boxWidth, y, ACCENT_GOLD, true);
    }

    private static void renderLeftDebugPanel(GuiGraphics guiGraphics, Minecraft mc) {
        int y = 10;
        int x = 10;
        String dimension = mc.level.dimension().location().getPath();
        double altitude = mc.player.getY();
        
        drawPanel(guiGraphics, "§6§lCOSMONAUTICS DEBUG SYSTEM", x, y, 0xFFFFFF);
        y += LINE_HEIGHT + 4;

        SubLevel ship = (SubLevel) Sable.HELPER.getContaining(mc.level, mc.player.blockPosition());
        if (ship != null) {
            drawPanel(guiGraphics, "§bSHIP DETECTED: §f" + ship.getUniqueId().toString().substring(0, 8), x, y, 0x55FFFF);
            y += LINE_HEIGHT;
            
            Vector3d velocity = new Vector3d(ship.logicalPose().position()).sub(ship.lastPose().position()).mul(20.0);
            drawPanel(guiGraphics, String.format("Speed: %.2f m/s", velocity.length()), x, y, 0x55FF55);
            y += LINE_HEIGHT;
        }

        drawPanel(guiGraphics, "Dim: " + dimension, x, y, 0xAAAAAA);
        y += LINE_HEIGHT;
        drawPanel(guiGraphics, String.format("Alt: %.2f", altitude), x, y, 0xAAAAAA);
        y += LINE_HEIGHT;
        
        // Asteroid Debug
        SubLevelContainer container = SubLevelContainer.getContainer(mc.level);
        if (container != null) {
            int asteroidCount = 0;
            double minDist = Double.MAX_VALUE;
            Vector3d playerPos = new Vector3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            
            for (SubLevel sl : container.getAllSubLevels()) {
                if (sl == ship) continue; // Ignore current ship
                
                // Check if it's an asteroid by name
                boolean isAsteroid = false;
                try {
                    String name = sl.getName();
                    if (name != null && name.contains("Asteroid")) {
                        isAsteroid = true;
                    }
                } catch (Throwable ignored) {}
                
                if (isAsteroid) {
                    asteroidCount++;
                    double dist = sl.logicalPose().position().distance(playerPos);
                    if (dist < minDist) minDist = dist;
                }
            }
            
            drawPanel(guiGraphics, "Asteroids: " + asteroidCount, x, y, 0xFFCC00);
            y += LINE_HEIGHT;
            if (asteroidCount > 0) {
                drawPanel(guiGraphics, String.format("Nearest: %.1fm", minDist), x, y, 0xFFCC00);
                y += LINE_HEIGHT;
            }
        }

        drawPanel(guiGraphics, "Ticks: " + seamlessTransitionTicks, x, y, 0xFF5555);
    }

    private static void renderRightLogPanel(GuiGraphics guiGraphics, Minecraft mc) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int y = 10;
        int x = screenWidth - 10;
        
        synchronized (debugLogs) {
            for (int i = debugLogs.size() - 1; i >= 0; i--) {
                LogEntry entry = debugLogs.get(i);
                int width = mc.font.width(entry.message);
                guiGraphics.fill(x - width - 4, y - 2, x + 2, y + 10, BG_COLOR);
                guiGraphics.drawString(mc.font, entry.message, x - width, y, entry.color, true);
                y += LINE_HEIGHT;
            }
        }
    }

    private static void drawPanel(GuiGraphics gui, String text, int x, int y, int color) {
        int width = Minecraft.getInstance().font.width(text);
        gui.fill(x - 4, y - 2, x + width + 4, y + 10, BG_COLOR);
        gui.fill(x - 4, y - 2, x - 2, y + 10, ACCENT_GOLD);
        gui.drawString(Minecraft.getInstance().font, text, x, y, color, true);
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
        
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.devce.rocketnautics.registry.RocketBlocks.SEPARATOR.get(), net.minecraft.client.renderer.RenderType.cutout());
        });
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

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!showDebugOverlay || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Render for all sub-levels in the current level
        SubLevelContainer container = SubLevelContainer.getContainer(mc.level);
        if (container == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        for (SubLevel ship : container.getAllSubLevels()) {
            if (ship.boundingBox().center(new Vector3d()).distance(new Vector3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())) < 200) {
                renderShipDebug3D(poseStack, ship);
            }
        }

        poseStack.popPose();
    }

    private static void renderShipDebug3D(PoseStack poseStack, SubLevel ship) {
        Pose3d pose = ship.logicalPose();
        Vector3d worldCoM = new Vector3d();
        ship.boundingBox().center(worldCoM);
        
        Vector3d velocity = new Vector3d(pose.position()).sub(ship.lastPose().position()).mul(20.0);
        double speed = velocity.length();

        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        float s = 1.5f;
        Matrix4f matrix = poseStack.last().pose();
        
        buffer.addVertex(matrix, (float)worldCoM.x - s, (float)worldCoM.y, (float)worldCoM.z).setColor(1f, 0.2f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x + s, (float)worldCoM.y, (float)worldCoM.z).setColor(1f, 0.2f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y - s, (float)worldCoM.z).setColor(0.2f, 1f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y + s, (float)worldCoM.z).setColor(0.2f, 1f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y, (float)worldCoM.z - s).setColor(0.2f, 0.2f, 1f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y, (float)worldCoM.z + s).setColor(0.2f, 0.2f, 1f, 1f);
        
        float b = 0.2f;
        drawDebugBox(buffer, matrix, worldCoM, b, 1f, 0.8f, 0f);

        // Velocity Vector (Yellow)
        if (speed > 0.1) {
            buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y, (float)worldCoM.z).setColor(1f, 1f, 0f, 1f);
            buffer.addVertex(matrix, (float)(worldCoM.x + velocity.x), (float)(worldCoM.y + velocity.y), (float)(worldCoM.z + velocity.z)).setColor(1f, 1f, 0f, 1f);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
    }

    private static void drawDebugBox(BufferBuilder buffer, Matrix4f matrix, Vector3d pos, float s, float r, float g, float b) {
        float x = (float)pos.x; float y = (float)pos.y; float z = (float)pos.z;
        // Edges
        buffer.addVertex(matrix, x-s, y-s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x+s, y-s, z-s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x+s, y-s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x+s, y+s, z-s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x+s, y+s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x-s, y+s, z-s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x-s, y+s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x-s, y-s, z-s).setColor(r, g, b, 1f);
        
        buffer.addVertex(matrix, x-s, y-s, z+s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x+s, y-s, z+s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x+s, y-s, z+s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x+s, y+s, z+s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x+s, y+s, z+s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x-s, y+s, z+s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x-s, y+s, z+s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x-s, y-s, z+s).setColor(r, g, b, 1f);
        
        buffer.addVertex(matrix, x-s, y-s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x-s, y-s, z+s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x+s, y-s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x+s, y-s, z+s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x+s, y+s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x+s, y+s, z+s).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x-s, y+s, z-s).setColor(r, g, b, 1f); buffer.addVertex(matrix, x-s, y+s, z+s).setColor(r, g, b, 1f);
    }
}
