package dev.devce.rocketnautics;

import net.minecraft.client.DeltaTracker;

import dev.devce.rocketnautics.content.blocks.VectorThrusterRenderer;
import dev.devce.rocketnautics.content.particles.RocketExhaustParticle;
import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.client.RocketSettingsScreen;
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
 * Main client-side class for Cosmonautics.
 * Handles debug overlays, rendering layers, particle registration, and 3D debug visualizations for ships.
 */
@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RocketNauticsClient {

    private static final int MAX_DEBUG_LOGS = 15;
    private static final int LINE_HEIGHT = 12;
    private static final int BG_COLOR = 0xCC1A1A1A; 
    private static final int ACCENT_GOLD = 0xFFFFD700; 
    private static final int PANEL_BORDER = 0xEE333333;

    /** Original render distance of the user before mod-induced changes. */
    public static int originalRenderDistance = -1;
    /** The last render distance value that was programmatically applied. */
    public static int lastAppliedRenderDistance = -1;
    
    /** Key mapping to toggle the jetpack on/off. */
    public static final net.minecraft.client.KeyMapping JETPACK_TOGGLE = new net.minecraft.client.KeyMapping(
            "key.rocketnautics.toggle_jetpack",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_G,
            "key.categories.rocketnautics"
    );
    
    /** Timer for the seamless atmosphere-to-space transition effect. */
    public static int seamlessTransitionTicks = 0;
    
    /** Checks if the debug overlay is enabled in the config. */
    public static boolean isDebugOverlayEnabled() {
        return RocketConfig.CLIENT.showDebugOverlay.get();
    }
    
    private static final List<LogEntry> debugLogs = Collections.synchronizedList(new ArrayList<>());

    public record LogEntry(String message, int color, long timestamp) {}

    public static void addLog(String message, int color) {
        debugLogs.add(new LogEntry(message, color, System.currentTimeMillis()));
        if (debugLogs.size() > MAX_DEBUG_LOGS) {
            debugLogs.remove(0);
        }
        RocketNautics.LOGGER.info("[RENDER INFO] {}", message);
    }

    /**
     * Triggers the seamless transition sequence, reducing render distance to 
     * maintain performance during world/dimension swaps.
     */
    public static void startSeamlessTransition() {
        Minecraft mc = Minecraft.getInstance();
        if (originalRenderDistance == -1) {
            originalRenderDistance = mc.options.renderDistance().get();
        }
        mc.options.renderDistance().set(2);
        lastAppliedRenderDistance = 2;
        seamlessTransitionTicks = 100; 
    }

    /** Ends the seamless transition sequence. */
    public static void endSeamlessTransition() {
        seamlessTransitionTicks = 0;
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "transition_overlay"), 
                RocketNauticsClient::renderDebugOverlays);
    }

    /**
     * Renders all enabled debug overlays (status bar, left panel, right logs).
     */
    private static void renderDebugOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !isDebugOverlayEnabled()) return;

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
        
        
        SubLevelContainer container = SubLevelContainer.getContainer(mc.level);
        if (container != null) {
            int asteroidCount = 0;
            double minDist = Double.MAX_VALUE;
            Vector3d playerPos = new Vector3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            
            for (SubLevel sl : container.getAllSubLevels()) {
                if (sl == ship) continue; 
                
                
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
        
        
        net.neoforged.fml.ModLoadingContext.get().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, 
            () -> (client, parent) -> new RocketSettingsScreen(parent));

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

    /**
     * Renders 3D debug visualizations for nearby SubLevels (ships/asteroids) during the level render stage.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!isDebugOverlayEnabled() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        
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

    /**
     * Renders a 3D coordinate cross, velocity vector, landing path, and thruster vectors for a ship.
     */
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
        
        float s = 1.5f; // Scale of the cross
        Matrix4f matrix = poseStack.last().pose();
        
        // --- 1. Draw Ship Bounding Box (Cyan) ---
        var bb = ship.boundingBox();
        drawDebugBox(buffer, matrix, new Vector3d(bb.minX(), bb.minY(), bb.minZ()), new Vector3d(bb.maxX(), bb.maxY(), bb.maxZ()), 0f, 1f, 1f);

        // --- 2. Draw Center of Mass Axis (X-Red, Y-Green, Z-Blue) ---
        buffer.addVertex(matrix, (float)worldCoM.x - s, (float)worldCoM.y, (float)worldCoM.z).setColor(1f, 0.2f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x + s, (float)worldCoM.y, (float)worldCoM.z).setColor(1f, 0.2f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y - s, (float)worldCoM.z).setColor(0.2f, 1f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y + s, (float)worldCoM.z).setColor(0.2f, 1f, 0.2f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y, (float)worldCoM.z - s).setColor(0.2f, 0.2f, 1f, 1f);
        buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y, (float)worldCoM.z + s).setColor(0.2f, 0.2f, 1f, 1f);
        
        // --- 3. Draw Center Point Box (Orange) ---
        drawDebugBox(buffer, matrix, new Vector3d(worldCoM).sub(0.2, 0.2, 0.2), new Vector3d(worldCoM).add(0.2, 0.2, 0.2), 1f, 0.8f, 0f);

        // --- 4. Draw Velocity Vector (Yellow) ---
        if (speed > 0.1) {
            buffer.addVertex(matrix, (float)worldCoM.x, (float)worldCoM.y, (float)worldCoM.z).setColor(1f, 1f, 0f, 1f);
            buffer.addVertex(matrix, (float)(worldCoM.x + velocity.x), (float)(worldCoM.y + velocity.y), (float)(worldCoM.z + velocity.z)).setColor(1f, 1f, 0f, 1f);
            
            // --- 5. Draw Predicted Landing Path (Green) ---
            renderPredictedPath(buffer, matrix, worldCoM, velocity, 0.2f, 1f, 0.2f);
        }

        // --- 6. Draw Thruster Vectors (Purple) ---
        renderThrusterVectors(buffer, matrix, ship, pose);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
    }

    private static void renderPredictedPath(BufferBuilder buffer, Matrix4f matrix, Vector3d start, Vector3d velocity, float r, float g, float b) {
        Vector3d currentPos = new Vector3d(start);
        Vector3d currentVel = new Vector3d(velocity).div(20.0); // Velocity per tick
        
        for (int i = 0; i < 100; i++) {
            Vector3d nextPos = new Vector3d(currentPos).add(currentVel);
            
            // Apply simple gravity (approximated)
            currentVel.y -= 0.04; // Gravity constant approx
            currentVel.mul(0.98); // Drag constant approx
            
            buffer.addVertex(matrix, (float)currentPos.x, (float)currentPos.y, (float)currentPos.z).setColor(r, g, b, 0.5f);
            buffer.addVertex(matrix, (float)nextPos.x, (float)nextPos.y, (float)nextPos.z).setColor(r, g, b, 0.5f);
            
            currentPos.set(nextPos);
            
            // Stop if we hit "ground" (Y=0 or similar)
            if (currentPos.y < -64) break;
        }
    }

    private static void renderThrusterVectors(BufferBuilder buffer, Matrix4f matrix, SubLevel ship, Pose3d pose) {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        net.minecraft.world.level.ChunkPos min = ship.getPlot().getChunkMin();
        net.minecraft.world.level.ChunkPos max = ship.getPlot().getChunkMax();
        
        double originX = min.x * 16.0;
        double originZ = min.z * 16.0;

        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;

                for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof dev.devce.rocketnautics.content.blocks.RocketThrusterBlockEntity thruster && thruster.isActive()) {
                        // Calculate world position of thruster
                        Vector3d localPos = new Vector3d(be.getBlockPos().getX() + 0.5 - originX, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5 - originZ);
                        Vector3d worldPos = new Vector3d(localPos).rotate(pose.orientation()).add(pose.position());
                        
                        // Get thrust direction
                        net.minecraft.core.Direction dir = thruster.getThrustDirection().getOpposite();
                        double power = thruster.getCurrentPower() / 10.0;
                        
                        Vector3d thrustDir = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ()).rotate(pose.orientation()).mul(power);
                        
                        // Draw vector (Purple)
                        buffer.addVertex(matrix, (float)worldPos.x, (float)worldPos.y, (float)worldPos.z).setColor(0.8f, 0.2f, 1f, 1f);
                        buffer.addVertex(matrix, (float)(worldPos.x + thrustDir.x), (float)(worldPos.y + thrustDir.y), (float)(worldPos.z + thrustDir.z)).setColor(0.8f, 0.2f, 1f, 1f);
                    }
                }
            }
        }
    }

    private static void drawDebugBox(BufferBuilder buffer, Matrix4f matrix, Vector3d min, Vector3d max, float r, float g, float b) {
        float x1 = (float)min.x; float y1 = (float)min.y; float z1 = (float)min.z;
        float x2 = (float)max.x; float y2 = (float)max.y; float z2 = (float)max.z;
        
        // Bottom
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 1f); buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, 1f); buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, 1f); buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, 1f); buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 1f);
        
        // Top
        buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, 1f); buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, 1f); buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 1f); buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, 1f); buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, 1f);
        
        // Pillars
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 1f); buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, 1f); buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, 1f); buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 1f);
        buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, 1f); buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, 1f);
    }
}
