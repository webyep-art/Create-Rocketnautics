package dev.devce.rocketnautics.content.blocks.nodes.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import dev.devce.rocketnautics.content.blocks.nodes.NodeConnection;
import dev.devce.rocketnautics.content.blocks.nodes.NodeGraph;
import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.api.nodes.NodeRegistry;
import net.minecraft.resources.ResourceLocation;
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
    private final List<NodeHandler> filteredHandlers = new ArrayList<>();
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

    // Value Editing
    private boolean isEditingValue = false;
    private String valueBuffer = "";

    // Tabs
    public enum ScreenTab { EDITOR, PERIPHERALS }
    private ScreenTab activeTab = ScreenTab.EDITOR;

    // Node Documentation
    private boolean isNodeDocsOpen = false;
    private Node docsNode = null;

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
            sputnik.refreshPeripherals();
        }

        if (minecraft.level.getGameTime() % 20 == 0 && isSearching) {
            updateSearch();
        }
        for (Node node : graph.nodes) {
            if (node.typeId.toString().endsWith("link_output")) {
                double val = graph.evaluate(node, sputnik);
                dev.devce.rocketnautics.content.blocks.nodes.LinkedSignalHandler.setSignal(sputnik.getLevel(), node.freqStack1, node.freqStack2, sputnik.getBlockPos(), val);
            }
        }
        
        graphics.fill(0, 0, width, height, 0xFF121212);
        
        // Tab Bar
        graphics.fill(0, 0, width, 24, 0xFF121212); // Slightly darker top bar
        graphics.fill(0, 23, width, 24, 0xFF2A2A2A); // Subtle separator

        // --- Left Side: Tabs ---
        // Editor Tab
        int editorTabX = 15;
        int editorTabW = font.width("EDITOR");
        boolean hoverEditor = mouseX >= editorTabX && mouseX <= editorTabX + editorTabW && mouseY >= 4 && mouseY <= 20;
        int editorColor = activeTab == ScreenTab.EDITOR ? 0xFF00FF88 : (hoverEditor ? 0xFFFFFFFF : 0xFF888888);
        graphics.drawString(font, "§lEDITOR", editorTabX, 8, editorColor);
        if (activeTab == ScreenTab.EDITOR) graphics.fill(editorTabX - 2, 22, editorTabX + editorTabW + 2, 24, 0xFF00FF88);

        // Peripherals Tab
        int periTabX = editorTabX + editorTabW + 25;
        int periTabW = font.width("PERIPHERALS");
        boolean hoverPeri = mouseX >= periTabX && mouseX <= periTabX + periTabW && mouseY >= 4 && mouseY <= 20;
        int periColor = activeTab == ScreenTab.PERIPHERALS ? 0xFF00FF88 : (hoverPeri ? 0xFFFFFFFF : 0xFF888888);
        graphics.drawString(font, "§lPERIPHERALS", periTabX, 8, periColor);
        if (activeTab == ScreenTab.PERIPHERALS) graphics.fill(periTabX - 2, 22, periTabX + periTabW + 2, 24, 0xFF00FF88);

        if (activeTab == ScreenTab.EDITOR) {
            renderEditor(graphics, mouseX, mouseY, deltaTime);
        } else {
            renderPeripheralsTab(graphics, mouseX, mouseY);
        }
        
        graphics.pose().popPose(); // End of intro scale

        // --- Right Side: Action Buttons (Matching Tab Style) ---
        int rightX = width - 15;

        // Docs button
        String docsText = "DOCS";
        int docsW = font.width(docsText);
        int docsX = rightX - docsW;
        boolean hoverDocs = mouseX >= docsX && mouseX <= docsX + docsW && mouseY >= 4 && mouseY <= 20;
        graphics.drawString(font, "§l" + docsText, docsX, 8, hoverDocs ? 0xFF00FF88 : 0xFF44AA66);
        if (hoverDocs) graphics.fill(docsX - 2, 22, docsX + docsW + 2, 24, 0xFF00FF88);

        // Copy button
        String copyText = "COPY";
        int copyW = font.width(copyText);
        int copyX = docsX - copyW - 20;
        boolean hoverCopy = mouseX >= copyX && mouseX <= copyX + copyW && mouseY >= 4 && mouseY <= 20;
        graphics.drawString(font, "§l" + copyText, copyX, 8, hoverCopy ? 0xFF55AAFF : 0xFF3366BB);
        if (hoverCopy) graphics.fill(copyX - 2, 22, copyX + copyW + 2, 24, 0xFF55AAFF);

        // Paste button
        String pasteText = "PASTE";
        int pasteW = font.width(pasteText);
        int pasteX = copyX - pasteW - 20;
        boolean hoverPaste = mouseX >= pasteX && mouseX <= pasteX + pasteW && mouseY >= 4 && mouseY <= 20;
        graphics.drawString(font, "§l" + pasteText, pasteX, 8, hoverPaste ? 0xFFFFAA00 : 0xFFBB7700);
        if (hoverPaste) graphics.fill(pasteX - 2, 22, pasteX + pasteW + 2, 24, 0xFFFFAA00);
    }

    private void renderEditor(GuiGraphics graphics, double mouseX, double mouseY, float deltaTime) {
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

            NodeHandler hFrom = from.getHandler();
            int color = hFrom != null ? hFrom.getHeaderColor() : 0xFFAAAAAA;
            double value = graph.evaluate(from, sputnik);
            boolean active = value > 0.5;
            
            if (active) {
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                color = 0xFF000000 | (Math.min(255, r + 100) << 16) | (Math.min(255, g + 100) << 8) | Math.min(255, b + 100);
            }

            int x1 = from.x + 100;
            int y1 = from.y + 18;
            int x2 = to.x;
            
            NodeHandler toHandler = to.getHandler();
            int y2 = to.y + 18;
            if (toHandler != null) {
                y2 = to.y + 18 + conn.targetPin * 12;
            }

            boolean highlighted = false;
            int wx = (int)(mouseX - panX);
            int wy = (int)(mouseY - panY);
            if ((Math.abs(wx - x1) < 5 && Math.abs(wy - y1) < 5) || (Math.abs(wx - x2) < 5 && Math.abs(wy - y2) < 5)) {
                highlighted = true;
            }

            if (active || highlighted) {
                drawSmoothCurve(graphics, x1, y1, x2, y2, color, highlighted ? 4.0f : 3.0f);
            }
            drawSmoothCurve(graphics, x1, y1, x2, y2, color, 1.0f);
        }

        if (linkingNode != null) {
            int sx = linkingNode.x + 100;
            int sy = linkingNode.y + 18;
            int tx = (int)(mouseX - panX);
            int ty = (int)(mouseY - panY);
            drawSmoothCurve(graphics, sx, sy, tx, ty, 0xAAFFFFFF, 1.0f);
        }

        int zLevel = 0;
        for (Node node : graph.nodes) {
            if (node != selectedNode) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, zLevel++ * 5);
                drawNode(graphics, node);
                graphics.pose().popPose();
            }
        }
        if (selectedNode != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 1000);
            drawNode(graphics, selectedNode);
            graphics.pose().popPose();
        }

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

        if (isNodeDocsOpen && docsNode != null) {
            renderNodeDocs(graphics);
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
    }

    private void renderPeripheralsTab(GuiGraphics graphics, double mouseX, double mouseY) {
        int x = 20;
        int y = 40;
        graphics.drawString(font, "§lSYSTEM DIAGNOSTICS - PERIPHERALS", x, y, 0xFF00FF88);
        graphics.fill(x, y + 12, x + 250, y + 13, 0xFF00FF88);
        y += 25;

        List<dev.devce.rocketnautics.api.peripherals.IPeripheral> peripherals = sputnik.getPeripherals();
        graphics.drawString(font, "Active Modules: " + peripherals.size(), x, y, 0xFFAAAAAA);
        y += 20;

        for (int i = 0; i < peripherals.size(); i++) {
            dev.devce.rocketnautics.api.peripherals.IPeripheral p = peripherals.get(i);
            String type = p.getPeripheralType();
            double thrust = p.readValue("thrust") / 100.0;
            
            int color = thrust > 0.01 ? 0xFF00FF88 : 0xFFFFAA00;
            String typeLabel = switch(type) {
                case "booster" -> "§6[BOOSTER]";
                case "vector_engine" -> "§b[VECTOR]";
                case "rcs" -> "§d[RCS]";
                default -> "§7[ENGINE]";
            };

            // Background
            graphics.fill(x, y, x + 250, y + 30, 0x11FFFFFF);
            graphics.renderOutline(x, y, 250, 30, 0x33FFFFFF);

            // ID and Type
            graphics.drawString(font, "ID:" + i + " " + typeLabel, x + 5, y + 5, 0xFFFFFFFF);
            
            // Thrust/Status
            String status = String.format("%.0f%% Thrust", thrust * 100);
            if (type.equals("booster")) {
                double fuel = p.readValue("fuel");
                status = fuel > 0 ? String.format("§e%.0fs Burn", fuel / 20.0) : "§cSPENT";
            }
            graphics.drawString(font, status, x + 180, y + 5, color);

            // Additional Data
            if (type.equals("vector_engine")) {
                double gx = p.readValue("gimbal_x");
                double gz = p.readValue("gimbal_z");
                graphics.drawString(font, String.format("Gimbal: X:%.2f Z:%.2f", gx, gz), x + 10, y + 18, 0xFF888888);
            } else if (type.equals("booster")) {
                boolean ignited = p.readValue("ignited") > 0.5;
                graphics.drawString(font, ignited ? "§aIGNITED" : "§7READY", x + 10, y + 18, 0xFFAAAAAA);
            } else {
                graphics.drawString(font, "Pos: " + p.getBlockPos().toShortString(), x + 10, y + 18, 0xFF888888);
            }

            y += 35;
            if (y > height - 40) break;
        }
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

    private void renderNodeDocs(GuiGraphics graphics) {
        if (docsNode == null) return;
        NodeHandler h = docsNode.getHandler();
        if (h == null) return;

        int w = 220;
        int h_box = 140;
        int x = width / 2 - w / 2;
        int y = height / 2 - h_box / 2;

        graphics.fill(x, y, x + w, y + h_box, 0xEE121212);
        graphics.renderOutline(x, y, w, h_box, h.getHeaderColor());
        
        graphics.drawString(font, "§8CATEGORY: " + h.getCategory().toUpperCase(), x + 10, y + 10, 0xFFFFFFFF);
        graphics.drawString(font, "§6" + h.getDisplayName().getString(), x + 10, y + 22, 0xFFFFFFFF);
        graphics.fill(x + 10, y + 34, x + w - 10, y + 35, 0x33FFFFFF);
        
        String desc = h.getDescription().getString();
        int ty = y + 40;
        
        // Simple line wrapping
        String[] words = desc.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.contains("\n")) {
                String[] parts = word.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        graphics.drawString(font, line.toString(), x + 10, ty, 0xFFAAAAAA);
                        line = new StringBuilder();
                        ty += 10;
                    }
                    if (font.width(line + " " + parts[i]) > w - 20) {
                        graphics.drawString(font, line.toString(), x + 10, ty, 0xFFAAAAAA);
                        line = new StringBuilder(parts[i]);
                        ty += 10;
                    } else {
                        if (line.length() > 0) line.append(" ");
                        line.append(parts[i]);
                    }
                }
            } else if (font.width(line + " " + word) > w - 20) {
                graphics.drawString(font, line.toString(), x + 10, ty, 0xFFAAAAAA);
                line = new StringBuilder(word);
                ty += 10;
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }
        graphics.drawString(font, line.toString(), x + 10, ty, 0xFFAAAAAA);

        graphics.drawCenteredString(font, "§8[ Click anywhere to close ]", x + w / 2, y + h_box - 12, 0xFFFFFFFF);
    }

    private void renderSearchMenu(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 2000);
        
        int menuWidth = 120;
        int itemHeight = 14;
        int maxVisibleItems = 10;
        int visibleHeight = 20 + Math.min(filteredHandlers.size(), maxVisibleItems) * itemHeight;

        // Ensure scroll is within bounds
        int maxScroll = Math.max(0, filteredHandlers.size() - maxVisibleItems);
        if (searchScroll > maxScroll) searchScroll = maxScroll;
        if (searchScroll < 0) searchScroll = 0;

        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + visibleHeight, 0xEE1A1A1A);
        graphics.renderOutline(menuX, menuY, menuWidth, visibleHeight, 0xFF444444);
        
        graphics.drawString(font, "> " + searchQuery + "_", menuX + 5, menuY + 5, 0xFF00FF88);
        
        graphics.enableScissor(menuX, menuY + 18, menuX + menuWidth, menuY + visibleHeight - 2);

        for (int i = 0; i < filteredHandlers.size(); i++) {
            NodeHandler handler = filteredHandlers.get(i);
            int ty = menuY + 20 + (i - searchScroll) * itemHeight;
            
            if (ty + itemHeight > menuY + 18 && ty < menuY + visibleHeight) {
                boolean hover = mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= ty && mouseY <= ty + itemHeight;
                graphics.drawString(font, handler.getDisplayName().getString(), menuX + 5, ty + 2, hover ? 0xFFFFFFFF : 0xFFAAAAAA);
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
        sputnik.refreshPeripherals();
        filteredHandlers.clear();
        String q = searchQuery.toLowerCase();
        for (NodeHandler handler : NodeRegistry.REGISTRY) {
            if (handler.getDisplayName().getString().toLowerCase().contains(q)) {
                if (handler.isAvailable(sputnik)) {
                    filteredHandlers.add(handler);
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
        NodeHandler handler = node.getHandler();
        
        if (node.typeId.toString().endsWith("comment")) {
            // ... (comment rendering logic remains similar but uses typeId)
            int bgColor = 0xFF444422;
            int borderColor = (node == selectedNode) ? 0xFFFFFFFF : 0xFFCCCC33;
            if (node == focusedCommentNode) borderColor = 0xFF00FF88;

            graphics.fill(node.x, node.y, node.x + 120, node.y + 60, bgColor);
            graphics.renderOutline(node.x, node.y, 120, 60, borderColor);
            graphics.drawString(font, "§6[ COMMENT ]", node.x + 5, node.y + 5, 0xFFFFFFFF);
            
            String text = node.commentText;
            if (node == focusedCommentNode && (System.currentTimeMillis() / 500) % 2 == 0) text += "_";
            
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
            if (ty <= node.y + 50) graphics.drawString(font, line.toString(), node.x + 5, ty, 0xFFAAAAAA);
            return;
        }

        if (handler == null) return;

        int width = getNodeWidth(node);
        int baseHeight = 40 + (Math.max(handler.getInputCount(), handler.getOutputCount()) - 1) * 12;
        
        double currentVal = (handler.getCategory().equals("Wireless")) ? node.value : graph.evaluate(node, sputnik);
        
        int customHeight = getCustomUIHeight(node);
        int height = baseHeight + customHeight;

        boolean isSelected = node == selectedNode;
        boolean isActiveOutput = Math.abs(currentVal) > 0.001;

        graphics.fill(node.x + 2, node.y + 2, node.x + width + 2, node.y + height + 2, 0xAA000000);
        graphics.fill(node.x, node.y, node.x + width, node.y + height, isSelected ? 0xEE252525 : 0xDD1A1A1A);
        
        int accentColor = handler.getHeaderColor();
        graphics.fill(node.x, node.y, node.x + width, node.y + 1, accentColor);
        graphics.renderOutline(node.x, node.y, width, height, isSelected ? 0xFFFFFFFF : 0xFF444444);
        
        graphics.fill(node.x + 1, node.y + 1, node.x + width - 1, node.y + 13, 0x44000000);
        String title = handler.getIcon() + " " + handler.getDisplayName().getString();
        graphics.drawString(font, title, node.x + 5, node.y + 3, 0xFFE0E0E0);

        // Help Icon
        int hx = node.x + width - 10;
        int hy = node.y + 3;
        graphics.drawString(font, "§7?", hx, hy, 0xFFFFFFFF);

        // Render Custom UI if present
        handler.renderCustomUI(graphics, node, node.x, node.y + baseHeight - 5, width, currentVal, 0);
        
        // Value Display
        String info = String.format("%.2f", currentVal);
        if (node.typeId.toString().endsWith("constant")) {
            if (isEditingValue && node == selectedNode) {
                info = "> " + valueBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            } else {
                info = node.value == (long)node.value ? String.valueOf((long)node.value) : String.format("%.2f", node.value);
            }
        } else if (node.typeId.toString().contains("thruster") || 
                   node.typeId.toString().contains("booster") || 
                   node.typeId.toString().contains("vector") || 
                   node.typeId.toString().contains("rcs")) {
            if (isEditingValue && node == selectedNode) {
                info = "IDX: " + valueBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            } else {
                info = "IDX: " + node.engineIndex;
            }
        } else if (node.typeId.toString().endsWith("math")) {
            info = "§6" + node.operation;
        }
        graphics.drawCenteredString(font, info, node.x + width / 2, node.y + 22, isSelected ? 0xFFFFFFFF : 0xFFAAAAAA);

        // Input pins (left)
        int inCount = handler.getInputCount();
        java.util.List<net.minecraft.network.chat.Component> inNames = handler.getInputNames();
        for (int i = 0; i < inCount; i++) {
            int py = node.y + 16 + i * 12;
            renderPin(graphics, node.x - 4, py, handler.getPinColor(i), isInputConnected(node.id, i));
            
            // Render label if available
            if (i < inNames.size()) {
                graphics.drawString(font, "§8" + inNames.get(i).getString(), node.x + 4, py - 2, 0xFFFFFFFF);
            }
        }
        
        // Output pins (right)
        int outCount = handler.getOutputCount();
        java.util.List<net.minecraft.network.chat.Component> outNames = handler.getOutputNames();
        for (int i = 0; i < outCount; i++) {
            int py = node.y + 16 + i * 12;
            renderPin(graphics, node.x + width - 1, py, accentColor, isOutputConnected(node.id));
            
            // Render label if available (right aligned)
            if (i < outNames.size()) {
                String label = outNames.get(i).getString();
                graphics.drawString(font, "§8" + label, node.x + width - font.width(label) - 6, py - 2, 0xFFFFFFFF);
            }
        }
    }

    private boolean isInputConnected(UUID nodeId, int pin) {
        for (NodeConnection c : graph.connections) if (c.targetNode.equals(nodeId) && c.targetPin == pin) return true;
        return false;
    }
    private boolean isOutputConnected(UUID nodeId) {
        for (NodeConnection c : graph.connections) if (c.sourceNode.equals(nodeId)) return true;
        return false;
    }

    /** Compute node display width based on title and pin label text widths. */
    private int getNodeWidth(Node node) {
        NodeHandler h = node.getHandler();
        if (h == null) return 100;
        String title = h.getIcon() + " " + h.getDisplayName().getString();
        int w = font.width(title) + 30; // Extra padding for help icon and icons
        // Also ensure pin labels fit
        for (Component c : h.getInputNames())  w = Math.max(w, font.width(c.getString()) * 2 + 40);
        for (Component c : h.getOutputNames()) w = Math.max(w, font.width(c.getString()) * 2 + 40);
        return Math.max(w, 100);
    }

    /** Custom UI extra height below base node area. */
    private int getCustomUIHeight(Node node) {
        NodeHandler h = node.getHandler();
        if (h == null) return 0;
        if (node.typeId.toString().contains("vector_control")) return 50;
        if (node.typeId.toString().contains("booster"))        return 25;
        if (node.typeId.toString().contains("link_"))           return 30;
        if (node.typeId.toString().contains("data_"))           return 20;
        if (h.getCategory().equals("Sensors"))                 return 30;
        if (node.typeId.toString().contains("attitude"))       return 95;
        return 0;
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
        // Tab switching
        if (mouseY >= 0 && mouseY <= 24) {
            if (mouseX >= 10 && mouseX <= 80) {
                activeTab = ScreenTab.EDITOR;
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            if (mouseX >= 90 && mouseX <= 180) {
                activeTab = ScreenTab.PERIPHERALS;
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2F));
                return true;
            }
        }

        if (activeTab == ScreenTab.PERIPHERALS) return false;

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
            int visibleHeight = 20 + Math.min(filteredHandlers.size(), maxVisibleItems) * itemHeight;

            if (mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= menuY + 20 && mouseY <= menuY + visibleHeight) {
                int idx = (int) ((mouseY - menuY - 20) / itemHeight) + searchScroll;
                if (idx >= 0 && idx < filteredHandlers.size()) {
                    NodeHandler handler = filteredHandlers.get(idx);
                    int[] pos = findFreePosition((int) (menuX - panX), (int) (menuY - panY));
                    graph.nodes.add(new Node(NodeRegistry.getId(handler), pos[0], pos[1]));
                    isSearching = false;
                    return true;
                }
            }
            isSearching = false;
            return true;
        }

        if (button == 0) { // Left click
            if (isNodeDocsOpen) {
                isNodeDocsOpen = false;
                return true;
            }
            selectedNode = null;
            // Check pins and node bodies
            for (Node node : graph.nodes) {
                NodeHandler h = node.getHandler();
                if (h == null) continue;

                int nodeW = getNodeWidth(node);
                int nodeH = 40 + (Math.max(h.getInputCount(), h.getOutputCount()) - 1) * 12 + getCustomUIHeight(node);

                // Help Icon Check
                if (worldX >= node.x + nodeW - 12 && worldX <= node.x + nodeW && worldY >= node.y && worldY <= node.y + 12) {
                    isNodeDocsOpen = true;
                    docsNode = node;
                    return true;
                }

                // Output pin click to start connection
                int outCount = h.getOutputCount();
                for (int i = 0; i < outCount; i++) {
                    int px = node.x + getNodeWidth(node) - 1;
                    int py = node.y + 16 + i * 12;
                    if (worldX >= px - 4 && worldX <= px + 4 && worldY >= py - 4 && worldY <= py + 4) {
                        linkingNode = node;
                        linkingPin = i;
                        return true;
                    }
                }
                
                // Input pin click to finish connection
                if (linkingNode != null && linkingNode != node) {
                    int targetPin = -1;
                    int inCount = h.getInputCount();
                    for (int i = 0; i < inCount; i++) {
                        int px = node.x - 4;
                        int py = node.y + 16 + i * 12;
                        if (worldX >= px - 4 && worldX <= px + 4 && worldY >= py - 4 && worldY <= py + 4) {
                            targetPin = i;
                            break;
                        }
                    }
                    
                    if (targetPin != -1) {
                        graph.connections.add(new NodeConnection(linkingNode.id, linkingPin, node.id, targetPin));
                        spawnSparks((int)worldX, (int)worldY, linkingNode.getHandler().getHeaderColor());
                        syncWithServer();
                        linkingNode = null;
                        return true;
                    }
                }

                // Node header drag zone (top 14 pixels)
                if (worldX >= node.x && worldX <= node.x + nodeW && worldY >= node.y && worldY <= node.y + 14) {
                    selectedNode = node;
                    draggingNode = node;
                    dragOffsetX = (int) (worldX - node.x);
                    dragOffsetY = (int) (worldY - node.y);
                    return true;
                }

                // Node body click (interaction)
                if (worldX >= node.x && worldX <= node.x + nodeW && worldY >= node.y + 14 && worldY <= node.y + nodeH) {
                    if (node.typeId.toString().contains("comment") || node.typeId.toString().contains("data_")) {
                        isTypingComment = true;
                        focusedCommentNode = node;
                        return true;
                    }
                    
                    if (node.typeId.toString().endsWith("constant")) {
                        isEditingValue = true;
                        valueBuffer = node.value == (long)node.value ? String.valueOf((long)node.value) : String.valueOf(node.value);
                        selectedNode = node;
                        return true;
                    }

                    if (node.typeId.toString().endsWith("math")) {
                        node.operation = switch (node.operation) {
                            case "+" -> "-";
                            case "-" -> "*";
                            case "*" -> "/";
                            case "/" -> "%";
                            case "%" -> "^";
                            case "^" -> ">";
                            case ">" -> "<";
                            case "<" -> "==";
                            case "==" -> "!=";
                            case "!=" -> ">=";
                            case ">=" -> "<=";
                            default -> "+";
                        };
                        spawnSparks((int)worldX, (int)worldY, 0xFFAAAAFF);
                        syncWithServer();
                        return true;
                    }

                    // Peripheral ID selection (Thruster, Booster, Vector, RCS)
                    String type = node.typeId.toString();
                    if (type.contains("thruster") || type.contains("booster") || type.contains("vector") || type.contains("rcs")) {
                        // Detect click on the "IDX: X" area (usually top middle area)
                        isEditingValue = true;
                        valueBuffer = String.valueOf(node.engineIndex);
                        selectedNode = node;
                        return true;
                    }

                    // Linked Frequency Slots
                    if (type.contains("link_")) {
                        int baseH = 40 + (Math.max(h.getInputCount(), h.getOutputCount()) - 1) * 12;
                        int slotSize = 18;
                        int padding = 5;
                        int startX = node.x + nodeW / 2 - slotSize - padding / 2;
                        int startY = node.y + baseH - 5 + 5; // Custom UI starts at baseHeight - 5
                        
                        if (worldX >= startX && worldX <= startX + slotSize && worldY >= startY && worldY <= startY + slotSize) {
                            startItemSearch(node, 1);
                            return true;
                        }
                        
                        int startX2 = startX + slotSize + padding;
                        if (worldX >= startX2 && worldX <= startX2 + slotSize && worldY >= startY && worldY <= startY + slotSize) {
                            startItemSearch(node, 2);
                            return true;
                        }
                    }

                    selectedNode = node;
                    draggingNode = node;
                    dragOffsetX = (int) (worldX - node.x);
                    dragOffsetY = (int) (worldY - node.y);
                    return true;
                }
            }
            linkingNode = null;
        } else if (button == 1) { // Right click
            for (Node node : graph.nodes) {
                NodeHandler h = node.getHandler();
                if (h == null) continue;

                // Input pins deletion
                for (int i = 0; i < h.getInputCount(); i++) {
                    int px = node.x - 4;
                    int py = node.y + 16 + i * 12;
                    if (worldX >= px - 4 && worldX <= px + 4 && worldY >= py - 4 && worldY <= py + 4) {
                        final int tp = i;
                        spawnSparks((int)worldX, (int)worldY, 0xFFFF5555);
                        graph.connections.removeIf(c -> c.targetNode.equals(node.id) && c.targetPin == tp);
                        syncWithServer();
                        return true;
                    }
                }
                // Output pins deletion
                for (int i = 0; i < h.getOutputCount(); i++) {
                    int px = node.x + getNodeWidth(node) - 1;
                    int py = node.y + 16 + i * 12;
                    if (worldX >= px - 4 && worldX <= px + 4 && worldY >= py - 4 && worldY <= py + 4) {
                        spawnSparks((int)worldX, (int)worldY, 0xFFFF5555);
                        final int sp = i;
                        graph.connections.removeIf(c -> c.sourceNode.equals(node.id) && c.sourcePin == sp);
                        syncWithServer();
                        return true;
                    }
                }
            }

            isSearching = true;
            menuX = (int) mouseX;
            menuY = (int) mouseY;
            searchQuery = "";
            updateSearch();
            return true;
        } else if (button == 2) { // Middle click delete
            Node toRemove = null;
            for (Node node : graph.nodes) {
                if (worldX >= node.x && worldX <= node.x + getNodeWidth(node) && worldY >= node.y && worldY <= node.y + 40 + getCustomUIHeight(node)) {
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
        if (activeTab == ScreenTab.PERIPHERALS) return false;
        if (draggingNode != null) {
            int grid = 10;
            draggingNode.x = (int) (Math.round((mouseX - panX - dragOffsetX) / grid) * grid);
            draggingNode.y = (int) (Math.round((mouseY - panY - dragOffsetY) / grid) * grid);
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
        if (activeTab == ScreenTab.PERIPHERALS) return false;
        if (isSearching) {
            searchScroll = Math.max(0, searchScroll - (int) scrollY);
            return true;
        }

        int worldX = (int) (mouseX - panX);
        int worldY = (int) (mouseY - panY);

        for (Node node : graph.nodes) {
            NodeHandler h = node.getHandler();
            if (h == null) continue;
            
            // Interaction for numeric and ID nodes
            if (node.typeId.toString().contains("constant") || node.typeId.toString().contains("throttle")) {
                int nodeW = getNodeWidth(node);
                int nodeH = 40 + getCustomUIHeight(node);
                
                if (worldX >= node.x && worldX <= node.x + nodeW && worldY >= node.y && worldY <= node.y + nodeH) {
                    if (node.typeId.toString().contains("constant")) {
                        double step = hasShiftDown() ? 1000.0 : 10.0;
                        node.value = node.value + (scrollY > 0 ? step : -step);
                    } else if (node.typeId.toString().contains("throttle")) {
                        int step = hasShiftDown() ? 10 : 1; 
                        node.engineIndex = Math.max(0, node.engineIndex + (scrollY > 0 ? step : -step));
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
        if (isEditingValue && selectedNode != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!valueBuffer.isEmpty()) {
                    valueBuffer = valueBuffer.substring(0, valueBuffer.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                try {
                    if (selectedNode.typeId.toString().endsWith("throttle")) {
                        selectedNode.engineIndex = Integer.parseInt(valueBuffer);
                    } else {
                        selectedNode.value = Double.parseDouble(valueBuffer);
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, ignore
                }
                isEditingValue = false;
                syncWithServer();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isEditingValue = false;
                return true;
            }
        }
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
                if (keyCode == GLFW.GLFW_KEY_ENTER && !filteredHandlers.isEmpty()) {
                    NodeHandler handler = filteredHandlers.get(0);
                    int[] pos = findFreePosition((int) (menuX - panX), (int) (menuY - panY));
                    graph.nodes.add(new Node(NodeRegistry.getId(handler), pos[0], pos[1]));
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
            Node copy = new Node(selectedNode.typeId, pos[0], pos[1]);
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
        int width = getNodeWidth(node);
        int height = 40 + getCustomUIHeight(node);
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
        if (isEditingValue) {
            if (Character.isDigit(codePoint) || codePoint == '.' || codePoint == '-') {
                valueBuffer += codePoint;
            }
            return true;
        }
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
