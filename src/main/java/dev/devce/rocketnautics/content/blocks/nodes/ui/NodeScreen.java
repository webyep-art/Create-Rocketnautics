package dev.devce.rocketnautics.content.blocks.nodes.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import dev.devce.rocketnautics.content.blocks.nodes.NodeConnection;
import dev.devce.rocketnautics.content.blocks.nodes.NodeGraph;
import dev.devce.rocketnautics.content.blocks.nodes.NodeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.CompoundTag;

import dev.devce.rocketnautics.content.blocks.SputnikBlockEntity;
import dev.devce.rocketnautics.network.SputnikNodeSyncPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class NodeScreen extends Screen {
    private final NodeGraph graph;
    private final SputnikBlockEntity sputnik;
    
    // Viewport panning
    private double panX = 0;
    private double panY = 0;
    private boolean isPanning = false;

    // Selection and interaction
    private Node selectedNode = null;
    private Node draggingNode = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // Connection drawing
    private Node linkingNode = null;
    private int linkingPin = -1; // 0=output
    private int mouseX, mouseY;

    // Search state
    private boolean isSearching = false;
    private String searchQuery = "";
    private int menuX, menuY;
    private final List<NodeType> filteredTypes = new ArrayList<>();
    private int searchScroll = 0;

    // Item Search state
    private boolean isSearchingItem = false;
    private String searchItemText = "";
    private Node searchingNode = null;
    private int searchingSlot = 0; // 1 or 2
    private List<net.minecraft.world.item.Item> filteredItems = new ArrayList<>();

    // Help and Sharing state
    private boolean isHelpOpen = false;
    private boolean isTypingComment = false;
    private Node focusedCommentNode = null;

    // Peripheral Selection
    private boolean isSelectingEngine = false;
    private Node selectingForNode = null;

    // Visual Effects
    private float screenAnimation = 0.0f;
    private long lastFrameTime = 0;
    private float menuAnimation = 0.0f;
    private static class NodeParticle {
        double x, y, vx, vy;
        int color;
        int life, maxLife;
    }
    private final List<NodeParticle> editorParticles = new ArrayList<>();

    public NodeScreen(NodeGraph graph, SputnikBlockEntity sputnik) {
        super(Component.literal("Flight Computer Node Editor"));
        this.graph = graph;
        this.sputnik = sputnik;
    }

    @Override
    protected void init() {
        super.init();
        // Custom sidebar rendering instead of standard widgets
    }

    private void syncWithServer() {
        PacketDistributor.sendToServer(new SputnikNodeSyncPayload(sputnik.getBlockPos(), graph.save(minecraft.level.registryAccess())));
    }

    @Override
    public void onClose() {
        syncWithServer();
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Do nothing to skip vanilla blur and gradient
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = net.minecraft.Util.getMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        
        screenAnimation = Math.min(1.0f, screenAnimation + deltaTime * 4.0f);
        
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        graphics.pose().pushPose();
        // Intro Animation: slight scale up and fade
        float sOut = 0.98f + 0.02f * screenAnimation;
        graphics.pose().translate(width / 2f, height / 2f, 0);
        graphics.pose().scale(sOut, sOut, 1.0f);
        graphics.pose().translate(-width / 2f, -height / 2f, 0);
        // CRITICAL: Clear cache and process wireless signals so the editor shows REAL-TIME values
        graph.clearCache();
        
        // Refresh engines on client periodically to save performance (still feels real-time at 5-10 ticks)
        if (minecraft.level.getGameTime() % 10 == 0) {
            sputnik.refreshEngines();
        }

        if (minecraft.level.getGameTime() % 20 == 0 && isSearching) {
            updateSearch();
        }
        for (dev.devce.rocketnautics.content.blocks.nodes.Node node : graph.nodes) {
            if (node.type == dev.devce.rocketnautics.content.blocks.nodes.NodeType.LINK_OUTPUT) {
                double val = graph.evaluate(node, sputnik);
                dev.devce.rocketnautics.content.blocks.nodes.LinkedSignalHandler.setSignal(sputnik.getLevel(), node.freqStack1, node.freqStack2, sputnik.getBlockPos(), val);
            }
        }
        
        graphics.fill(0, 0, width, height, 0xFF121212);
        
        // Render Grid
        int gridSize = 20;
        int offsetX = (int) (panX % gridSize);
        int offsetY = (int) (panY % gridSize);
        for (int i = offsetX - gridSize; i < width; i += gridSize) {
            graphics.fill(i, 0, i + 1, height, 0x1AFFFFFF);
        }
        for (int i = offsetY - gridSize; i < height; i += gridSize) {
            graphics.fill(0, i, width, i + 1, 0x1AFFFFFF);
        }
        
        // Scanlines CRT Effect
        for (int i = 0; i < height; i += 2) {
            graphics.fill(0, i, width, i + 1, 0x0A000000);
        }
        
        graphics.pose().pushPose();
        graphics.pose().translate(panX, panY, 0);

        // Draw connections
        for (NodeConnection conn : graph.connections) {
            Node from = graph.getNode(conn.sourceNode);
            Node to = graph.getNode(conn.targetNode);
            if (from == null || to == null) continue;

            // Connection color depends on the OUTPUT of the source node
            int color = getOutputColor(from.type);
            double value = graph.evaluate(from, sputnik);
            boolean active = value > 0.5;
            
            // If active, use a glowing version of the color
            if (active) {
                // Mix with white to brighten
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                color = 0xFF000000 | (Math.min(255, r + 100) << 16) | (Math.min(255, g + 100) << 8) | Math.min(255, b + 100);
            }

            int x1 = from.x + 100;
            int y1 = from.y + 18; // Output pin center
            int x2 = to.x;
            int y2 = to.y + (to.type == NodeType.GIMBAL_SET ? (conn.targetPin == 0 ? 30 : 44) : (conn.targetPin == 0 ? 18 : 28));
            if (to.type == NodeType.LINK_INPUT || to.type == NodeType.LINK_OUTPUT) if (conn.targetPin == 1) y2 = to.y + 36;
            if (to.type == NodeType.MEMORY && conn.targetPin == 2) y2 = to.y + 32;

            // Highlight if pin is hovered
            boolean highlighted = false;
            int wx = (int)(mouseX - panX);
            int wy = (int)(mouseY - panY);
            if ((Math.abs(wx - x1) < 5 && Math.abs(wy - y1) < 5) || (Math.abs(wx - x2) < 5 && Math.abs(wy - y2) < 5)) {
                highlighted = true;
            }

            // Draw shadow/glow first
            if (active || highlighted) {
                drawSmoothCurve(graphics, x1, y1, x2, y2, color, highlighted ? 4.0f : 3.0f); // Thick glow
            }
            drawSmoothCurve(graphics, x1, y1, x2, y2, color, 1.0f); // Core line
        }

        // Draw actively linking connection (with SNAP logic)
        if (linkingNode != null) {
            int sx = linkingNode.x + 100;
            int sy = linkingNode.y + 18;
            int tx = (int)(mouseX - panX);
            int ty = (int)(mouseY - panY);
            drawSmoothCurve(graphics, sx, sy, tx, ty, 0xAAFFFFFF, 1.0f);
        }

        // Draw Nodes with proper Z-layering
        int zLevel = 0;
        for (Node node : graph.nodes) {
            if (node != selectedNode) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, zLevel++ * 5);
                drawNode(graphics, node);
                graphics.pose().popPose();
            }
        }
        // Selected node on top of everything
        if (selectedNode != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 1000); // Very high Z
            drawNode(graphics, selectedNode);
            graphics.pose().popPose();
        }

        // Update and draw particles (within pan context)
        for (int i = editorParticles.size() - 1; i >= 0; i--) {
            NodeParticle p = editorParticles.get(i);
            p.x += p.vx * deltaTime * 60.0;
            p.y += p.vy * deltaTime * 60.0;
            p.vx *= Math.pow(0.92, deltaTime * 60.0);
            p.vy *= Math.pow(0.92, deltaTime * 60.0);
            p.life -= deltaTime * 60.0;
            if (p.life <= 0) {
                editorParticles.remove(i);
                continue;
            }
            float alpha = (float) p.life / p.maxLife;
            int rColor = (p.color & 0xFFFFFF) | ((int)(alpha * 255 * screenAnimation) << 24);
            graphics.fill((int)p.x, (int)p.y, (int)p.x + 2, (int)p.y + 2, rColor);
            if (p.life > p.maxLife / 2) {
                graphics.fill((int)p.x - 1, (int)p.y - 1, (int)p.x + 3, (int)p.y + 3, (rColor & 0x33FFFFFF));
            }
        }

        graphics.pose().popPose();
        
        // Render Overlays on absolute top layer
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 2000);
        
        if (isSearching) {
            renderSearchMenu(graphics);
        } else if (isSearchingItem) {
            renderItemSearchMenu(graphics);
        }
        
        if (isHelpOpen) {
            renderHelpOverlay(graphics);
        }

        if (isSelectingEngine) {
            renderPeripheralList(graphics);
        }
        
        graphics.pose().popPose();
        
        // Final screen fade-in overlay
        if (screenAnimation < 1.0f) {
            int overlayAlpha = (int)((1.0f - screenAnimation) * 255);
            graphics.fill(0, 0, width, height, (overlayAlpha << 24) | 0x121212);
        }
        
        graphics.pose().popPose(); // End of intro scale

        // Docs button (Top Right)
        int docsX = width - 40;
        int docsY = 10;
        boolean hoverDocs = mouseX >= docsX && mouseX <= docsX + 30 && mouseY >= docsY && mouseY <= docsY + 14;
        graphics.fill(docsX, docsY, docsX + 30, docsY + 14, hoverDocs ? 0xFF555555 : 0xFF333333);
        graphics.renderOutline(docsX, docsY, 30, 14, 0xFF777777);
        graphics.drawCenteredString(font, "DOCS", docsX + 15, docsY + 3, 0xFF00FF88);

        // Copy button
        int copyX = docsX - 40;
        boolean hoverCopy = mouseX >= copyX && mouseX <= copyX + 35 && mouseY >= docsY && mouseY <= docsY + 14;
        graphics.fill(copyX, docsY, copyX + 35, docsY + 14, hoverCopy ? 0xFF555555 : 0xFF333333);
        graphics.renderOutline(copyX, docsY, 35, 14, 0xFF777777);
        graphics.drawCenteredString(font, "COPY", copyX + 17, docsY + 3, 0xFF00AAFF);

        // Paste button
        int pasteX = copyX - 45;
        boolean hoverPaste = mouseX >= pasteX && mouseX <= pasteX + 40 && mouseY >= docsY && mouseY <= docsY + 14;
        graphics.fill(pasteX, docsY, pasteX + 40, docsY + 14, hoverPaste ? 0xFF555555 : 0xFF333333);
        graphics.renderOutline(pasteX, docsY, 40, 14, 0xFF777777);
        graphics.drawCenteredString(font, "PASTE", pasteX + 20, docsY + 3, 0xFFFFAA00);
    }

    private void renderPeripheralList(GuiGraphics graphics) {
        int w = 150;
        int h = 120;
        int x = 10;
        int y = height - h - 10;

        graphics.fill(x, y, x + w, y + h, 0xDD121212);
        graphics.renderOutline(x, y, w, h, 0xFF00FF88);
        graphics.drawString(font, "--- PERIPHERALS ---", x + 5, y + 5, 0xFF00FF88);
        
        int ty = y + 20;
        int count = sputnik.getEngineCount();
        graphics.drawString(font, "Found Engines: " + count, x + 5, ty, 0xFFAAAAAA);
        ty += 12;

        for (int i = 0; i < count; i++) {
            String name = "ID:" + i + " Thruster";
            double thrust = sputnik.getEngineThrust(i);
            boolean active = thrust > 0.01;
            int color = active ? 0xFF00FF88 : 0xFFFF5555;
            
            String status = active ? "ACTIVE" : "IDLE";
            if (thrust > 0 && thrust < 1) status = "STARTING";
            
            graphics.drawString(font, name + " [" + status + "]", x + 10, ty, color);
            graphics.drawString(font, String.format("  Thrust: %.1f%%", thrust), x + 10, ty + 10, 0xFF888888);
            
            boolean hover = mouseX >= x + 10 && mouseX <= x + 140 && mouseY >= ty && mouseY <= ty + 18;
            if (hover) {
                graphics.fill(x + 10, ty, x + 140, ty + 18, 0x33FFFFFF);
                if (net.minecraft.client.Minecraft.getInstance().mouseHandler.isLeftPressed()) {
                    if (selectingForNode != null) {
                        selectingForNode.engineIndex = i;
                        isSelectingEngine = false;
                        syncWithServer();
                    }
                }
            }
            ty += 20;
            if (ty > y + h - 10) break;
        }
    }

    private void renderHelpOverlay(GuiGraphics graphics) {
        int w = 300;
        int h = 280;
        int x = width / 2 - w / 2;
        int y = height / 2 - h / 2;

        graphics.fill(x, y, x + w, y + h, 0xFF121212); // Fully opaque
        graphics.renderOutline(x, y, w, h, 0xFF00FF88);
        
        graphics.drawCenteredString(font, "--- SPUTNIK FLIGHT COMPUTER MANUAL ---", x + w / 2, y + 10, 0xFF00FF88);
        
        int ty = y + 30;
        String[] lines = {
            "§b[CONTROLS]",
            "• §fShift+A or Right Click §7- Open Search Menu",
            "• §fDrag Node §7- Move around",
            "• §fRight Click Pin §7- Start connection",
            "• §fShift+Click Node §7- Change mode / value",
            "• §fMiddle Click §7- Delete node",
            "",
            "§e[WIRELESS LINKING]",
            "• §fBlue/Red Slots §7- Set items for frequency",
            "• §fParity §7- Works with Create Redstone Links",
            "• §fDistance §7- Follows Create range rules",
            "",
            "§c[REDSTONE OUTPUT]",
            "• §fAnalog §7- Values 0-15 set signal strength",
            "• §fBoolean §7- 1.0 (True) sets strength to 15",
            "",
            "§a[TIPS]",
            "• Use §fMEMORY §7nodes for state toggle",
            "• Use §fADVANCED §7for math functions like SIN/COS",
            "• Flight data is updated every tick (20Hz)"
        };

        for (String line : lines) {
            graphics.drawString(font, line, x + 10, ty, 0xFFFFFFFF);
            ty += 11; // Slightly more spacing
        }

        graphics.drawCenteredString(font, "[ Click anywhere to close ]", x + w / 2, y + h - 15, 0xFF555555);
    }

    private void renderSearchMenu(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 2000);
        
        int menuWidth = 120;
        int itemHeight = 14;
        int maxVisibleItems = 10;
        int visibleHeight = 20 + Math.min(filteredTypes.size(), maxVisibleItems) * itemHeight;

        // Ensure scroll is within bounds
        int maxScroll = Math.max(0, filteredTypes.size() - maxVisibleItems);
        if (searchScroll > maxScroll) searchScroll = maxScroll;
        if (searchScroll < 0) searchScroll = 0;

        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + visibleHeight, 0xEE1A1A1A);
        graphics.renderOutline(menuX, menuY, menuWidth, visibleHeight, 0xFF444444);
        
        graphics.drawString(font, "> " + searchQuery + "_", menuX + 5, menuY + 5, 0xFF00FF88);
        
        // Content with Scissor
        int scissorY = (int) ((height - (menuY + visibleHeight)) * (double) minecraft.getWindow().getGuiScaledHeight() / height); // This is complex due to screen scaling
        // Easier way: use GuiGraphics.enableScissor (NeoForge/Minecraft 1.21.1)
        graphics.enableScissor(menuX, menuY + 18, menuX + menuWidth, menuY + visibleHeight - 2);

        for (int i = 0; i < filteredTypes.size(); i++) {
            NodeType type = filteredTypes.get(i);
            int ty = menuY + 20 + (i - searchScroll) * itemHeight;
            
            // Only render if within visible area
            if (ty + itemHeight > menuY + 18 && ty < menuY + visibleHeight) {
                boolean hover = mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= ty && mouseY <= ty + itemHeight;
                graphics.drawString(font, type.getDisplayName(), menuX + 5, ty + 2, hover ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
        }
        
        graphics.disableScissor();
        graphics.pose().popPose();
    }

    private void renderItemSearchMenu(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 2000);
        
        int w = 150;
        int h = 200;
        int x = width / 2 - w / 2;
        int y = height / 2 - h / 2;

        graphics.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        graphics.renderOutline(x, y, w, h, 0xFF444444);
        
        graphics.drawString(font, "> " + searchItemText + "_", x + 10, y + 10, 0xFF00FF88);
        graphics.fill(x + 10, y + 22, x + w - 10, y + 23, 0xFF444444);

        int count = 0;
        int row = 0;
        int col = 0;
        for (net.minecraft.world.item.Item item : filteredItems) {
            int ix = x + 10 + col * 20;
            int iy = y + 30 + row * 20;
            
            boolean hover = mouseX >= ix && mouseX <= ix + 18 && mouseY >= iy && mouseY <= iy + 18;
            if (hover) {
                graphics.fill(ix - 1, iy - 1, ix + 19, iy + 19, 0x44FFFFFF);
            }
            
            graphics.renderFakeItem(new net.minecraft.world.item.ItemStack(item), ix, iy);
            
            col++;
            if (col >= 6) {
                col = 0;
                row++;
            }
            count++;
            if (count >= 48) break; // Limit display
        }
        
        graphics.pose().popPose();
    }

    private void updateSearch() {
        sputnik.refreshEngines();
        filteredTypes.clear();
        String q = searchQuery.toLowerCase();
        for (NodeType type : NodeType.values()) {
            if (type.getDisplayName().toLowerCase().contains(q)) {
                // Only show peripheral nodes if engines are found
                if (type == NodeType.THRUST_GET || type == NodeType.THRUST_SET || type == NodeType.GIMBAL_SET || type == NodeType.ENGINE_ID || type == NodeType.PERIPHERAL_LIST) {
                    if (sputnik.getEngineCount() > 0) {
                        filteredTypes.add(type);
                    }
                } else {
                    filteredTypes.add(type);
                }
            }
        }
    }

    private int[] findFreePosition(int x, int y) {
        for (Node node : graph.nodes) {
            // Aggressive margin (130x80) to prevent title collisions
            if (Math.abs(node.x - x) < 130 && Math.abs(node.y - y) < 80) {
                return findFreePosition(x + 30, y + 30);
            }
        }
        return new int[]{x, y};
    }

    private void drawNode(GuiGraphics graphics, Node node) {
        if (node.type == NodeType.COMMENT) {
            int bgColor = 0xFF444422; // Dark yellow/orange
            int borderColor = 0xFFCCCC33;
            if (node == selectedNode) borderColor = 0xFFFFFFFF;
            if (node == focusedCommentNode) borderColor = 0xFF00FF88;

            graphics.fill(node.x, node.y, node.x + 120, node.y + 60, bgColor);
            graphics.renderOutline(node.x, node.y, 120, 60, borderColor);
            graphics.drawString(font, "§6[ COMMENT ]", node.x + 5, node.y + 5, 0xFFFFFFFF);
            
            // Render text with simple wrapping
            String text = node.commentText;
            if (node == focusedCommentNode && (System.currentTimeMillis() / 500) % 2 == 0) {
                text += "_";
            }
            
            int ty = node.y + 20;
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (font.width(line + " " + word) > 110) {
                    graphics.drawString(font, line.toString(), node.x + 5, ty, 0xFFAAAAAA);
                    line = new StringBuilder(word);
                    ty += 10;
                    if (ty > node.y + 50) break;
                } else {
                    if (line.length() > 0) line.append(" ");
                    line.append(word);
                }
            }
            if (ty <= node.y + 50) {
                graphics.drawString(font, line.toString(), node.x + 5, ty, 0xFFAAAAAA);
            }
            return;
        }

        int width = (node.type == NodeType.PERIPHERAL_LIST) ? 140 : 
                     (node.type.name().contains("THRUST") || node.type == NodeType.GIMBAL_SET || node.type == NodeType.ENGINE_ID || node.type == NodeType.OUTPUT || node.type.name().contains("LINK")) ? 130 : 100;
        int height = (node.type == NodeType.LINK_INPUT || node.type == NodeType.LINK_OUTPUT) ? 55 : 
                     (node.type == NodeType.GIMBAL_SET ? 85 : 
                     (node.type == NodeType.PERIPHERAL_LIST ? 40 + Math.min(12, sputnik.getEngineCount()) * 10 : 40));
        boolean isSelected = node == selectedNode;
        double currentVal = graph.evaluate(node, sputnik);
        boolean isActiveOutput = Math.abs(currentVal) > 0.001;

        // 1. Shadow
        graphics.fill(node.x + 2, node.y + 2, node.x + width + 2, node.y + height + 2, 0xAA000000);
        
        // 2. Glassmorphic Body (Semi-transparent with deep color)
        int bodyColor = isSelected ? 0xEE252525 : 0xDD1A1A1A;
        graphics.fill(node.x, node.y, node.x + width, node.y + height, bodyColor);
        
        // 3. Header Accent (Top gradient-like line)
        int accentColor = getHeaderColor(node.type);
        graphics.fill(node.x, node.y, node.x + width, node.y + 1, accentColor);
        
        // 4. Border (Techy look)
        int borderColor = isSelected ? 0xFFFFFFFF : 0xFF444444;
        graphics.renderOutline(node.x, node.y, width, height, borderColor);
        
        // 5. Header Section
        graphics.fill(node.x + 1, node.y + 1, node.x + width - 1, node.y + 13, 0x44000000);
        String title = getNodeIcon(node.type) + " " + node.type.getDisplayName();
        graphics.drawString(font, title, node.x + 5, node.y + 3, 0xFFE0E0E0);
        
        // 6. "Active" Status LED (Top Right) with pulse effect
        if (isActiveOutput) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 200.0));
            int pulsedColor = (accentColor & 0xFFFFFF) | ((int)(pulse * 255) << 24);
            graphics.fill(node.x + width - 8, node.y + 4, node.x + width - 4, node.y + 8, pulsedColor);
            graphics.fill(node.x + width - 9, node.y + 5, node.x + width - 3, node.y + 7, (accentColor & 0x44FFFFFF));
        } else {
            graphics.fill(node.x + width - 8, node.y + 4, node.x + width - 4, node.y + 8, 0xFF333333);
        }

        // Display Value/Config
        String info = "";
        switch (node.type) {
            case NUMBER_INPUT -> info = String.format("%.2f", node.value);
            case ALTITUDE -> info = String.format("%.1f m", sputnik.getAltitude());
            case VELOCITY -> info = String.format("%.1f m/s", sputnik.getVelocity());
            case PITCH -> info = String.format("%.1f" + (char)176, sputnik.getPitch());
            case YAW -> info = String.format("%.1f" + (char)176, sputnik.getYaw());
            case ROLL -> info = String.format("%.1f" + (char)176, sputnik.getRoll());
            case POS_X -> info = String.format("X: %.1f", sputnik.getX());
            case POS_Y -> info = String.format("Y: %.1f", sputnik.getY());
            case POS_Z -> info = String.format("Z: %.1f", sputnik.getZ());
            case THRUST_GET -> {
                double val = graph.evaluate(node, sputnik);
                info = String.format("ID:%d (%.0f%%)", node.engineIndex, val);
                graphics.drawString(font, "[L]", node.x + width - 20, node.y + 16, 0xFF00AAFF);
                int barW = width - 20;
                graphics.fill(node.x + 10, node.y + 30, node.x + 10 + barW, node.y + 33, 0xFF222222);
                graphics.fill(node.x + 10, node.y + 30, node.x + 10 + (int)(barW * Math.min(100.0, val) / 100.0), node.y + 33, 0xFF00FF88);
            }
            case THRUST_SET -> {
                info = String.format("ID:%d [THRUST]", node.engineIndex);
                graphics.drawString(font, "[L]", node.x + width - 20, node.y + 16, 0xFF00AAFF);
                double val = graph.getInputValue(node.id, 0, sputnik);
                int barW = width - 20;
                graphics.fill(node.x + 10, node.y + 30, node.x + 10 + barW, node.y + 33, 0xFF222222);
                graphics.fill(node.x + 10, node.y + 30, node.x + 10 + (int)(barW * Math.min(100.0, val) / 100.0), node.y + 33, 0xFF00AAFF);
            }
            case GIMBAL_SET -> {
                info = String.format("ID:%d [GIMBAL]", node.engineIndex);
                graphics.drawString(font, "[L]", node.x + width - 20, node.y + 16, 0xFF00AAFF);
                // Enhanced Crosshair Visual
                int cx = node.x + width / 2;
                int cy = node.y + 54;
                int size = 16;
                // Background grid
                graphics.fill(cx - size, cy - size, cx + size, cy + size, 0xFF111111);
                graphics.renderOutline(cx - size, cy - size, size * 2, size * 2, 0xFF333333);
                graphics.fill(cx - size, cy, cx + size, cy + 1, 0xFF222222);
                graphics.fill(cx, cy - size, cx + 1, cy + size, 0xFF222222);
                
                double gx = graph.getInputValue(node.id, 0, sputnik);
                double gz = graph.getInputValue(node.id, 1, sputnik);
                
                // Clamp visual within bounds
                int vx = (int)Math.max(-size+2, Math.min(size-2, gx / 5.0)); // 1 unit = 5 degrees for visual
                int vz = (int)Math.max(-size+2, Math.min(size-2, gz / 5.0));
                
                graphics.fill(cx + vx - 1, cy + vz - 1, cx + vx + 2, cy + vz + 2, 0xFF00FFFF);
                graphics.renderOutline(cx + vx - 2, cy + vz - 2, 5, 5, 0xFFFFFFFF);
            }
            case ENGINE_ID -> {
                info = String.format("INDEX: %d", node.engineIndex);
                graphics.drawString(font, "[L]", node.x + width - 20, node.y + 22, 0xFF00AAFF);
            }
            case COMPARE, LOGIC, MATH, ADVANCED -> {
                String valStr = Math.abs(currentVal) < 1000 ? String.format("%.2f", currentVal) : String.format("%.0f", currentVal);
                info = node.operation + " [" + valStr + "]";
            }
            case MEMORY -> info = String.format("VAL: %.2f", node.value);
            case LINK_INPUT, LINK_OUTPUT -> info = String.format("VAL: %.1f", currentVal);
            case OUTPUT -> {
                int strength = (currentVal == 1.0) ? 15 : (int) Math.max(0, Math.min(15, currentVal));
                info = node.selectedSide.toUpperCase() + " [" + strength + "]";
            }
        }
        
        if (!info.isEmpty() && node.type != NodeType.GIMBAL_SET) {
            graphics.drawCenteredString(font, info, node.x + width / 2, node.y + 22, 0xFFAAAAAA);
        } else if (node.type == NodeType.GIMBAL_SET) {
            graphics.drawCenteredString(font, info, node.x + width / 2, node.y + 16, 0xFFAAAAAA);
        }

        // Input pins (left)
        if (node.type.name().contains("SET") || node.type == NodeType.COMPARE || node.type == NodeType.LOGIC || node.type == NodeType.MATH || node.type == NodeType.ADVANCED || node.type == NodeType.MEMORY || node.type == NodeType.LINK_OUTPUT || node.type == NodeType.OUTPUT) {
            int inColor = getInputColor(node.type);
            if (node.type == NodeType.GIMBAL_SET) {
                renderPin(graphics, node.x - 4, node.y + 30, inColor, isInputConnected(node.id, 0));
                renderPin(graphics, node.x - 4, node.y + 44, inColor, isInputConnected(node.id, 1));
                graphics.drawString(font, "X", node.x + 4, node.y + 28, 0xAAFFFFFF);
                graphics.drawString(font, "Z", node.x + 4, node.y + 42, 0xAAFFFFFF);
            } else {
                renderPin(graphics, node.x - 4, node.y + 16, inColor, isInputConnected(node.id, 0));
                if (node.type == NodeType.COMPARE || node.type == NodeType.LOGIC || node.type == NodeType.MATH || node.type == NodeType.MEMORY) {
                    renderPin(graphics, node.x - 4, node.y + 28, inColor, isInputConnected(node.id, 1));
                }
            }
            if (node.type == NodeType.MEMORY) {
                renderPin(graphics, node.x - 4, node.y + 32, inColor, isInputConnected(node.id, 2));
            }
        }
        
        // Frequency Slots
        if (node.type == NodeType.LINK_INPUT || node.type == NodeType.LINK_OUTPUT) {
            int sx1 = node.x + width / 2 - 20;
            int sx2 = node.x + width / 2 + 2;
            int sy = node.y + 34;
            graphics.renderOutline(sx1, sy, 18, 18, 0xFF00AAFF);
            if (!node.freqStack1.isEmpty()) graphics.renderFakeItem(node.freqStack1, sx1 + 1, sy + 1);
            graphics.renderOutline(sx2, sy, 18, 18, 0xFFFF4444);
            if (!node.freqStack2.isEmpty()) graphics.renderFakeItem(node.freqStack2, sx2 + 1, sy + 1);
        }
        
        // Peripheral List content
        if (node.type == NodeType.PERIPHERAL_LIST) {
            int count = sputnik.getEngineCount();
            float scanPulse = (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 300.0));
            int scanColor = (0x00FF88 & 0xFFFFFF) | ((int)(scanPulse * 255) << 24);
            graphics.drawString(font, "ENGINES: " + count, node.x + 5, node.y + 18, 0xFF00FF88);
            graphics.drawString(font, "SCANNING...", node.x + 65, node.y + 18, scanColor);

            for (int i = 0; i < count && i < 12; i++) {
                net.minecraft.core.BlockPos p = sputnik.getEnginePos(i);
                if (p != null) {
                    graphics.drawString(font, String.format("ID:%d [%d,%d,%d]", i, p.getX(), p.getY(), p.getZ()), node.x + 5, node.y + 30 + i * 10, 0xFFAAAAAA);
                }
            }
        }
        
        // Output Pin (Right)
        if (node.type != NodeType.OUTPUT && node.type != NodeType.LINK_OUTPUT && node.type != NodeType.THRUST_SET && node.type != NodeType.GIMBAL_SET) {
            renderPin(graphics, node.x + width - 1, node.y + 16, getOutputColor(node.type), isOutputConnected(node.id));
        }
    }

    private int getHeaderColor(NodeType type) {
        return switch (type) {
            case NUMBER_INPUT, ALTITUDE, VELOCITY, PITCH, YAW, ROLL, POS_X, POS_Y, POS_Z -> 0xFF00AAFF;
            case COMPARE, LOGIC -> 0xFF00FF88;
            case MATH, ADVANCED -> 0xFFFFAA00;
            case MEMORY -> 0xFFFF00FF;
            case LINK_INPUT, LINK_OUTPUT -> 0xFFFFFF00;
            case THRUST_GET, THRUST_SET, GIMBAL_SET, ENGINE_ID -> 0xFF00FFFF;
            case OUTPUT -> 0xFFFF4444;
            default -> 0xFFFFFFFF;
        };
    }

    private boolean isInputConnected(UUID nodeId, int pin) {
        for (NodeConnection c : graph.connections) if (c.targetNode.equals(nodeId) && c.targetPin == pin) return true;
        return false;
    }

    private boolean isOutputConnected(UUID nodeId) {
        for (NodeConnection c : graph.connections) if (c.sourceNode.equals(nodeId)) return true;
        return false;
    }

    private void renderPin(GuiGraphics graphics, int x, int y, int color, boolean active) {
        // Outer socket
        graphics.fill(x - 1, y - 1, x + 5, y + 5, 0xFF333333);
        // Inner core
        graphics.fill(x, y, x + 4, y + 4, color);
        // Glow if connected
        if (active) {
            graphics.fill(x - 2, y, x - 1, y + 4, color & 0x44FFFFFF);
            graphics.fill(x + 5, y, x + 6, y + 4, color & 0x44FFFFFF);
            graphics.fill(x, y - 2, x + 4, y - 1, color & 0x44FFFFFF);
            graphics.fill(x, y + 5, x + 4, y + 6, color & 0x44FFFFFF);
        }
    }

    private int getInputColor(NodeType type) {
        return switch (type) {
            case COMPARE, MATH, ADVANCED, MEMORY, LINK_INPUT, LINK_OUTPUT, THRUST_SET, GIMBAL_SET -> 0xFF00AAFF; // Expects Numbers
            case LOGIC, OUTPUT -> 0xFF00FF88; // Expects Signal
            default -> 0xFFAAAAAA;
        };
    }

    private int getOutputColor(NodeType type) {
        return switch (type) {
            case NUMBER_INPUT, ALTITUDE, VELOCITY, PITCH, YAW, MATH, ADVANCED, MEMORY, LINK_INPUT, LINK_OUTPUT, POS_X, POS_Y, POS_Z -> 0xFF00AAFF; // Outputs Numbers
            case COMPARE, LOGIC -> 0xFF00FF88; // Outputs Signal
            default -> 0xFFAAAAAA;
        };
    }

    private String getNodeIcon(NodeType type) {
        return switch (type) {
            case NUMBER_INPUT -> "[#]";
            case ALTITUDE -> "[H]";
            case VELOCITY -> "[V]";
            case PITCH -> "[P]";
            case YAW -> "[Y]";
            case ROLL -> "[R]";
            case MATH -> "[M]";
            case ADVANCED -> "[F]";
            case LOGIC -> "[L]";
            case COMPARE -> "[C]";
            case MEMORY -> "[M]";
            case LINK_INPUT -> "[W]";
            case LINK_OUTPUT -> "[W]";
            case THRUST_GET -> "[T]";
            case THRUST_SET -> "[T]";
            case GIMBAL_SET -> "[G]";
            case ENGINE_ID -> "[ID]";
            case PERIPHERAL_LIST -> "[P]";
            case POS_X -> "[X]";
            case POS_Y -> "[Y]";
            case POS_Z -> "[Z]";
            case OUTPUT -> "[O]";
            case COMMENT -> "//";
            default -> "[ ]";
        };
    }

    private void drawSmoothCurve(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, float thickness) {
        int steps = 24;
        int lastX = x1;
        int lastY = y1;
        
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            // Quadratic Bezier: (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
            // We use P1 as a midpoint with horizontal offset for that "node" look
            float cx1 = x1 + (x2 - x1) * 0.5f;
            float cy1 = y1;
            float cx2 = x1 + (x2 - x1) * 0.5f;
            float cy2 = y2;
            
            // Cubic Bezier
            float mt = 1.0f - t;
            float x = mt * mt * mt * x1 + 3 * mt * mt * t * cx1 + 3 * mt * t * t * cx2 + t * t * t * x2;
            float y = mt * mt * mt * y1 + 3 * mt * mt * t * cy1 + 3 * mt * t * t * cy2 + t * t * t * y2;
            
            drawLine(graphics, lastX, lastY, (int)x, (int)y, color, thickness);
            lastX = (int)x;
            lastY = (int)y;
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, float thickness) {
        if (thickness <= 1.0f) {
            if (x1 == x2) graphics.fill(x1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2), color);
            else if (y1 == y2) graphics.fill(Math.min(x1, x2), y1, Math.max(x1, x2), y1 + 1, color);
            else {
                // Diagonal line approximation
                int dx = Math.abs(x2 - x1);
                int dy = Math.abs(y2 - y1);
                int steps = Math.max(dx, dy);
                for (int i = 0; i <= steps; i++) {
                    float t = (float) i / steps;
                    graphics.fill(x1 + (int)((x2 - x1) * t), y1 + (int)((y2 - y1) * t), x1 + (int)((x2 - x1) * t) + 1, y1 + (int)((y2 - y1) * t) + 1, color);
                }
            }
        } else {
            // Thick line (just multiple lines)
            for (float o = -thickness/2; o <= thickness/2; o += 0.5f) {
                drawLine(graphics, x1, (int)(y1 + o), x2, (int)(y2 + o), color, 1.0f);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHelpOpen) {
            isHelpOpen = false;
            return true;
        }

        int docsX = width - 40;
        int docsY = 10;
        if (mouseX >= docsX && mouseX <= docsX + 30 && mouseY >= docsY && mouseY <= docsY + 14) {
            isHelpOpen = true;
            return true;
        }

        // Close comment focus if clicking elsewhere
        if (isTypingComment) {
            isTypingComment = false;
            focusedCommentNode = null;
        }

        int copyX = docsX - 40;
        if (mouseX >= copyX && mouseX <= copyX + 35 && mouseY >= docsY && mouseY <= docsY + 14) {
            copyToClipboard();
            return true;
        }

        int pasteX = copyX - 45;
        if (mouseX >= pasteX && mouseX <= pasteX + 40 && mouseY >= docsY && mouseY <= docsY + 14) {
            pasteFromClipboard();
            return true;
        }

        int worldX = (int) (mouseX - panX);
        int worldY = (int) (mouseY - panY);

        if (isSearchingItem) {
            int w = 150;
            int h = 200;
            int x = width / 2 - w / 2;
            int y = height / 2 - h / 2;
            
            int count = 0;
            int row = 0;
            int col = 0;
            for (net.minecraft.world.item.Item item : filteredItems) {
                int ix = x + 10 + col * 20;
                int iy = y + 30 + row * 20;
                if (mouseX >= ix && mouseX <= ix + 18 && mouseY >= iy && mouseY <= iy + 18) {
                    if (searchingSlot == 1) searchingNode.freqStack1 = new net.minecraft.world.item.ItemStack(item);
                    else searchingNode.freqStack2 = new net.minecraft.world.item.ItemStack(item);
                    isSearchingItem = false;
                    return true;
                }
                col++;
                if (col >= 6) { col = 0; row++; }
                count++;
                if (count >= 48) break;
            }
            
            if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
                isSearchingItem = false;
            }
            return true;
        }

        if (isSearching) {
            int menuWidth = 120;
            int itemHeight = 14;
            int maxVisibleItems = 10;
            int visibleHeight = 20 + Math.min(filteredTypes.size(), maxVisibleItems) * itemHeight;

            if (mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= menuY + 20 && mouseY <= menuY + visibleHeight) {
                int idx = (int) ((mouseY - menuY - 20) / itemHeight) + searchScroll;
                if (idx >= 0 && idx < filteredTypes.size()) {
                    NodeType type = filteredTypes.get(idx);
                    int[] pos = findFreePosition((int) (menuX - panX), (int) (menuY - panY));
                    graph.nodes.add(new Node(type, pos[0], pos[1]));
                    isSearching = false;
                    return true;
                }
            }
            isSearching = false;
            return true;
        }

        if (button == 0) { // Left click
            selectedNode = null;
            // Check pins
            for (Node node : graph.nodes) {
                // Output pin click
                if (node.type != NodeType.OUTPUT && node.type != NodeType.COMMENT && node.type != NodeType.PERIPHERAL_LIST && node.type != NodeType.THRUST_SET && node.type != NodeType.GIMBAL_SET && worldX >= node.x + 96 && worldX <= node.x + 104 && worldY >= node.y + 16 && worldY <= node.y + 24) {
                    linkingNode = node;
                    linkingPin = 0;
                    return true;
                }
                
                // Input pin click to finish connection
                if (linkingNode != null && linkingNode != node) {
                    int targetPin = -1;
                    if (node.type == NodeType.GIMBAL_SET) {
                        if (worldX >= node.x - 6 && worldX <= node.x + 6) {
                            if (worldY >= node.y + 26 && worldY <= node.y + 34) targetPin = 0;
                            else if (worldY >= node.y + 40 && worldY <= node.y + 48) targetPin = 1;
                        }
                    } else {
                        // Standard nodes (Pin 0 at 16, Pin 1 at 28)
                        if (worldX >= node.x - 6 && worldX <= node.x + 6) {
                            if (worldY >= node.y + 12 && worldY <= node.y + 20) targetPin = 0;
                            else {
                                int py = node.y + (node.type == NodeType.LINK_INPUT || node.type == NodeType.LINK_OUTPUT ? 36 : 28);
                                if (node.type != NodeType.OUTPUT && node.type != NodeType.ADVANCED && node.type != NodeType.LINK_INPUT && node.type != NodeType.THRUST_SET && worldY >= py - 4 && worldY <= py + 4) targetPin = 1;
                                else if (node.type == NodeType.MEMORY && worldY >= node.y + 28 && worldY <= node.y + 36) targetPin = 2;
                            }
                        }
                    }
                    
                    if (targetPin != -1) {
                        graph.connections.add(new NodeConnection(linkingNode.id, 0, node.id, targetPin));
                        spawnSparks((int)worldX, (int)worldY, getOutputColor(linkingNode.type));
                        syncWithServer();
                        linkingNode = null;
                        return true;
                    }
                }

                // Node drag and cycle settings
                int nodeW = node.type == NodeType.COMMENT ? 120 : (node.type == NodeType.PERIPHERAL_LIST ? 150 : 100);
                int nodeH = (node.type == NodeType.LINK_INPUT || node.type == NodeType.LINK_OUTPUT) ? 55 : (node.type == NodeType.GIMBAL_SET ? 40 : (node.type == NodeType.COMMENT ? 60 : (node.type == NodeType.PERIPHERAL_LIST ? 120 : 40)));
                
                if (worldX >= node.x && worldX <= node.x + nodeW && worldY >= node.y && worldY <= node.y + nodeH) {
                    // Check for frequency slot clicks
                    if (node.type == NodeType.LINK_INPUT || node.type == NodeType.LINK_OUTPUT) {
                        int sx1 = node.x + 50 - 20;
                        int sx2 = node.x + 50 + 2;
                        int sy = node.y + 32;
                        if (worldX >= sx1 && worldX <= sx1 + 18 && worldY >= sy && worldY <= sy + 18) {
                            startItemSearch(node, 1);
                            return true;
                        }
                        if (worldX >= sx2 && worldX <= sx2 + 18 && worldY >= sy && worldY <= sy + 18) {
                            startItemSearch(node, 2);
                            return true;
                        }
                    }

                    if (node.type == NodeType.COMMENT) {
                        isTypingComment = true;
                        focusedCommentNode = node;
                    }

                    selectedNode = node;
                    if (worldY > node.y + 14) { // Clicked body, cycle settings
                        boolean shift = hasShiftDown();
                        switch (node.type) {
                            case NUMBER_INPUT -> {
                                double step = shift ? 100.0 : 1.0;
                                node.value = node.value + (button == 0 ? step : -step);
                            }
                            case COMPARE -> node.operation = switch (node.operation) {
                                case ">" -> shift ? "==" : "<";
                                case "<" -> shift ? ">" : "==";
                                default -> shift ? "<" : ">";
                            };
                            case LOGIC -> node.operation = node.operation.equals("AND") ? "OR" : "AND";
                            case MATH -> {
                                String[] ops = {"+", "-", "*", "/", "%", "POW", "MIN", "MAX"};
                                int idx = 0;
                                for(int i=0; i<ops.length; i++) if(ops[i].equals(node.operation)) idx = i;
                                int next = (idx + (shift ? -1 : 1)) % ops.length;
                                if (next < 0) next = ops.length - 1;
                                node.operation = ops[next];
                            }
                            case ADVANCED -> {
                                String[] ops = {"SIN", "COS", "ABS", "SQRT", "ROUND", "FLOOR", "CEIL"};
                                int idx = 0;
                                for(int i=0; i<ops.length; i++) if(ops[i].equals(node.operation)) idx = i;
                                int next = (idx + (shift ? -1 : 1)) % ops.length;
                                if (next < 0) next = ops.length - 1;
                                node.operation = ops[next];
                            }
                            case MEMORY -> node.operation = "LATCH";
                            case THRUST_GET, THRUST_SET, GIMBAL_SET, ENGINE_ID -> {
                                // Check for [L] button click
                                if (worldX >= node.x + width - 30 && worldX <= node.x + width - 5) {
                                    isSelectingEngine = true;
                                    selectingForNode = node;
                                    return true;
                                }
                                node.engineIndex = Math.max(0, node.engineIndex + (shift ? -1 : 1));
                                if (node.engineIndex > 100) node.engineIndex = 0; // Wrap if too high
                            }
                            case OUTPUT -> {
                                String[] sides = {"down", "up", "north", "south", "west", "east", "all"};
                                int idx = 0;
                                for(int i=0; i<7; i++) if(sides[i].equalsIgnoreCase(node.selectedSide)) idx = i;
                                int next = (idx + (shift ? -1 : 1)) % 7;
                                if (next < 0) next = 6;
                                node.selectedSide = sides[next];
                            }
                        }
                        syncWithServer();
                        return true;
                    }
                    
                    draggingNode = node;
                    dragOffsetX = (int) (worldX - node.x);
                    dragOffsetY = (int) (worldY - node.y);
                    return true;
                }
            }
            linkingNode = null;
        } else if (button == 1) { // Right click
            for (Node node : graph.nodes) {
                // Check input pins to clear them
                int targetPin = -1;
                if (worldX >= node.x - 6 && worldX <= node.x + 4 && worldY >= node.y + 14 && worldY <= node.y + 22) targetPin = 0;
                else {
                    int py = node.y + (node.type == NodeType.GIMBAL_SET ? 30 : (node.type == NodeType.LINK_INPUT || node.type == NodeType.LINK_OUTPUT ? 36 : 28));
                    if (node.type != NodeType.OUTPUT && worldX >= node.x - 6 && worldX <= node.x + 4 && worldY >= py - 2 && worldY <= py + 6) targetPin = 1;
                    else if (node.type == NodeType.MEMORY) {
                        int pcy = node.y + 32;
                        if (worldX >= node.x - 6 && worldX <= node.x + 4 && worldY >= pcy - 2 && worldY <= pcy + 6) targetPin = 2;
                    }
                }
                
                if (targetPin != -1) {
                    final int tp = targetPin;
                    spawnSparks((int)worldX, (int)worldY, 0xFFFF5555); // Red deletion sparks
                    graph.connections.removeIf(c -> c.targetNode.equals(node.id) && c.targetPin == tp);
                    syncWithServer();
                    return true;
                }
                
                // Check output pin to clear all outgoing connections
                if (node.type != NodeType.OUTPUT && node.type != NodeType.COMMENT && node.type != NodeType.PERIPHERAL_LIST && node.type != NodeType.THRUST_SET && node.type != NodeType.GIMBAL_SET && worldX >= node.x + 96 && worldX <= node.x + 104 && worldY >= node.y + 16 && worldY <= node.y + 24) {
                    spawnSparks((int)worldX, (int)worldY, 0xFFFF5555);
                    graph.connections.removeIf(c -> c.sourceNode.equals(node.id));
                    syncWithServer();
                    return true;
                }
            }

            isSearching = true;
            menuX = (int) mouseX;
            menuY = (int) mouseY;
            searchQuery = "";
            updateSearch();
            return true;
        } else if (button == 2) { // Middle click pan or delete
            Node toRemove = null;
            for (Node node : graph.nodes) {
                int nodeW = node.type == NodeType.COMMENT ? 120 : (node.type == NodeType.PERIPHERAL_LIST ? 150 : 100);
                int nodeH = (node.type == NodeType.LINK_INPUT || node.type == NodeType.LINK_OUTPUT) ? 55 : (node.type == NodeType.GIMBAL_SET ? 85 : (node.type == NodeType.COMMENT ? 60 : (node.type == NodeType.PERIPHERAL_LIST ? 120 : 40)));
                if (worldX >= node.x && worldX <= node.x + nodeW && worldY >= node.y && worldY <= node.y + nodeH) {
                    spawnDeletionSparks(node);
                    toRemove = node;
                    break;
                }
            }
            if (toRemove != null) {
                final java.util.UUID toRemoveId = toRemove.id;
                graph.nodes.remove(toRemove);
                graph.connections.removeIf(c -> c.sourceNode.equals(toRemoveId) || c.targetNode.equals(toRemoveId));
                syncWithServer();
                return true;
            }
            isPanning = true;
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingNode != null) syncWithServer();
        draggingNode = null;
        isPanning = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingNode != null) {
            draggingNode.x = (int) (mouseX - panX - dragOffsetX);
            draggingNode.y = (int) (mouseY - panY - dragOffsetY);
            return true;
        }
        if (isPanning) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isSearching) {
            searchScroll = Math.max(0, searchScroll - (int) scrollY);
            return true;
        }

        int worldX = (int) (mouseX - panX);
        int worldY = (int) (mouseY - panY);

        for (Node node : graph.nodes) {
            if (node.type == NodeType.NUMBER_INPUT || node.type == NodeType.THRUST_GET || node.type == NodeType.THRUST_SET || node.type == NodeType.GIMBAL_SET || node.type == NodeType.ENGINE_ID) {
                int nodeW = 100;
                int nodeH = (node.type == NodeType.GIMBAL_SET) ? 55 : 40;
                if (worldX >= node.x && worldX <= node.x + nodeW && worldY >= node.y && worldY <= node.y + nodeH) {
                    if (node.type == NodeType.NUMBER_INPUT) {
                        double step = hasShiftDown() ? 100.0 : 1.0;
                        node.value = node.value + (scrollY > 0 ? step : -step);
                    } else {
                        node.engineIndex = Math.max(0, node.engineIndex + (scrollY > 0 ? 1 : -1));
                    }
                    syncWithServer();
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isTypingComment && focusedCommentNode != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (focusedCommentNode.commentText.length() > 0) {
                    focusedCommentNode.commentText = focusedCommentNode.commentText.substring(0, focusedCommentNode.commentText.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isTypingComment = false;
                focusedCommentNode = null;
                syncWithServer();
                return true;
            }
        }
        if (isSearchingItem) {
            if (keyCode == 256) { // ESC
                isSearchingItem = false;
                return true;
            }
            if (keyCode == 259 && !searchItemText.isEmpty()) { // Backspace
                searchItemText = searchItemText.substring(0, searchItemText.length() - 1);
                updateFilteredItems();
                return true;
            }
            return true;
        }
        if (isSearching) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                if (keyCode == GLFW.GLFW_KEY_ENTER && !filteredTypes.isEmpty()) {
                    NodeType type = filteredTypes.get(0);
                    int[] pos = findFreePosition((int) (menuX - panX), (int) (menuY - panY));
                    graph.nodes.add(new Node(type, pos[0], pos[1]));
                    syncWithServer();
                }
                isSearching = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                updateSearch();
                return true;
            }
            return true;
        }

        // Deletion
        if (selectedNode != null && (keyCode == GLFW.GLFW_KEY_X || keyCode == GLFW.GLFW_KEY_DELETE)) {
            final java.util.UUID toDeleteId = selectedNode.id;
            spawnDeletionSparks(selectedNode);
            graph.nodes.remove(selectedNode);
            graph.connections.removeIf(c -> c.sourceNode.equals(toDeleteId) || c.targetNode.equals(toDeleteId));
            syncWithServer();
            selectedNode = null;
            return true;
        }

        // Duplication (Shift+D)
        if (selectedNode != null && keyCode == GLFW.GLFW_KEY_D && hasShiftDown()) {
            int[] pos = findFreePosition(selectedNode.x + 10, selectedNode.y + 10);
            Node copy = new Node(selectedNode.type, pos[0], pos[1]);
            copy.value = selectedNode.value;
            copy.operation = selectedNode.operation;
            copy.selectedSide = selectedNode.selectedSide;
            graph.nodes.add(copy);
            selectedNode = copy;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_A && hasShiftDown()) {
            isSearching = true;
            searchScroll = 0;
            menuX = (int) mouseX;
            menuY = (int) mouseY;
            searchQuery = "";
            updateSearch();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void spawnSparks(int x, int y, int color) {
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < 15; i++) {
            NodeParticle p = new NodeParticle();
            p.x = x;
            p.y = y;
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 1.0 + rand.nextDouble() * 3.0;
            p.vx = Math.cos(angle) * speed;
            p.vy = Math.sin(angle) * speed;
            p.color = color;
            p.maxLife = 10 + rand.nextInt(15);
            p.life = p.maxLife;
            editorParticles.add(p);
        }
    }

    private void spawnDeletionSparks(Node node) {
        java.util.Random rand = new java.util.Random();
        int width = (node.type == NodeType.PERIPHERAL_LIST) ? 140 : 100;
        int height = 40;
        for (int i = 0; i < 30; i++) {
            NodeParticle p = new NodeParticle();
            p.x = node.x + rand.nextInt(width);
            p.y = node.y + rand.nextInt(height);
            p.vx = (rand.nextDouble() - 0.5) * 4.0;
            p.vy = (rand.nextDouble() - 0.5) * 4.0;
            p.color = 0xFFFF5555; // Red for deletion
            p.maxLife = 15 + rand.nextInt(20);
            p.life = p.maxLife;
            editorParticles.add(p);
        }
    }

    private void startItemSearch(Node node, int slot) {
        isSearchingItem = true;
        searchItemText = "";
        searchingNode = node;
        searchingSlot = slot;
        updateFilteredItems();
    }

    private void updateFilteredItems() {
        filteredItems.clear();
        String query = searchItemText.toLowerCase();
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (query.isEmpty() || item.getDescriptionId().toLowerCase().contains(query) || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString().contains(query)) {
                filteredItems.add(item);
            }
            if (filteredItems.size() >= 48) break;
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (isTypingComment && focusedCommentNode != null) {
            if (focusedCommentNode.commentText.length() < 100) {
                focusedCommentNode.commentText += codePoint;
            }
            return true;
        }
        if (isSearchingItem) {
            searchItemText += codePoint;
            updateFilteredItems();
            return true;
        }
        if (isSearching) {
            searchQuery += codePoint;
            updateSearch();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void copyToClipboard() {
        try {
            CompoundTag tag = graph.save(minecraft.level.registryAccess());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.write(tag, new DataOutputStream(baos));
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            minecraft.keyboardHandler.setClipboard(base64);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pasteFromClipboard() {
        try {
            String base64 = minecraft.keyboardHandler.getClipboard();
            byte[] data = Base64.getDecoder().decode(base64);
            CompoundTag tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)));
            if (tag != null) {
                NodeGraph loaded = new NodeGraph(tag, minecraft.level.registryAccess());
                graph.nodes.clear();
                graph.connections.clear();
                graph.nodes.addAll(loaded.nodes);
                graph.connections.addAll(loaded.connections);
                graph.clearCache();
            }
        } catch (Exception e) {
            // Invalid data, ignore
        }
    }
}
