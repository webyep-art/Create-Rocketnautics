package dev.devce.websnodelib.client.ui;

import dev.devce.websnodelib.api.WGraph;
import dev.devce.websnodelib.api.WNode;
import dev.devce.websnodelib.api.WConnection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;
import java.util.Base64;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * The main GUI screen for editing node graphs.
 * Supports zooming, panning, multiple node selection, and real-time data flow visualization.
 */
public class WNodeScreen extends Screen {
    private final WGraph graph;
    
    // Viewport panning and zoom
    private double panX = 0;
    private double panY = 0;
    private boolean isPanning = false;
    private float zoom = 1.0f;

    // Interaction state
    private WNode selectedNode = null;
    private WNode draggingNode = null;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    // Connection state
    private WNode linkingNode = null;
    private int linkingPin = -1;
    private int mouseX, mouseY;
    
    // Selection state
    private boolean isSelecting = false;
    private double selStartX, selStartY, selEndX, selEndY;

    // Animation and Effects
    private float screenAnimation = 0.0f;
    private long lastFrameTime = 0;
    
    /**
     * Particle system for the background.
     */
    private static class NodeParticle {
        double x, y, vx, vy;
        int color;
        int life, maxLife;
    }
    private final java.util.List<NodeParticle> editorParticles = new java.util.ArrayList<>();
    
    private boolean isSearching = false;
    private String searchQuery = "";
    private int menuX, menuY;
    private List<ResourceLocation> filteredTypes = new ArrayList<>();
    private int searchScrollOffset = 0;
    private Consumer<CompoundTag> onSave;

    // Item Picker Overlay State
    private dev.devce.websnodelib.api.elements.WItemPicker activeItemPicker = null;
    private String itemSearchQuery = "";
    private List<net.minecraft.world.item.Item> filteredItems = new ArrayList<>();
    private int itemScrollOffset = 0;
    private int itemMenuX, itemMenuY;

    public WNodeScreen(Component title, WGraph graph, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> onSave) {
        super(title);
        this.graph = graph;
        this.onSave = onSave;
    }

    public WNodeScreen(WGraph graph) {
        this(Component.literal("Web's Node Editor"), graph, (tag) -> {});
    }

    @Override
    public void removed() {
        if (onSave != null) onSave.accept(graph.save());
        super.removed();
    }

    @Override
    protected void init() {
        super.init();
        screenAnimation = 0;
        graph.updateTopology();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graph.tick();

        long now = net.minecraft.Util.getMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        
        screenAnimation = Math.min(1.0f, screenAnimation + deltaTime * 4.0f);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        
        graphics.fill(0, 0, width, height, 0xFF121212);
        
        for (int i = 0; i < height; i += 2) {
            graphics.fill(0, i, width, i + 1, 0x0A000000);
        }

        graphics.pose().pushPose();
        float sOut = (0.98f + 0.02f * screenAnimation) * zoom;
        graphics.pose().translate(width / 2f, height / 2f, 0);
        graphics.pose().scale(sOut, sOut, 1.0f);
        graphics.pose().translate(-width / 2f, -height / 2f, 0);
        
        drawGrid(graphics);

        graphics.pose().translate(panX, panY, 0);

        for (WConnection conn : graph.getConnections()) {
            drawConnection(graphics, conn, partialTick);
        }

        if (linkingNode != null) {
            int sx = linkingNode.getX() + linkingNode.getWidth();
            int sy = linkingNode.getY() + 18 + linkingPin * 12;
            int tx = (int)((mouseX - width / 2f) / zoom + width / 2f - panX);
            int ty = (int)((mouseY - height / 2f) / zoom + height / 2f - panY);
            drawSmoothCurve(graphics, sx, sy, tx, ty, 0xAAFFFFFF, 1.5f);
        }

        int z = 0;
        for (WNode node : graph.getNodes()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, z++ * 10); 
            node.render(graphics, (int)((mouseX - width / 2f) / zoom + width / 2f - panX), (int)((mouseY - height / 2f) / zoom + height / 2f - panY), partialTick);
            graphics.pose().popPose();
        }

        if (isSelecting) {
            float x1 = (float)Math.min(selStartX, selEndX);
            float y1 = (float)Math.min(selStartY, selEndY);
            float x2 = (float)Math.max(selStartX, selEndX);
            float y2 = (float)Math.max(selStartY, selEndY);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 2000);
            graphics.fill((int)x1, (int)y1, (int)x2, (int)y2, 0x3300FF88);
            graphics.renderOutline((int)x1, (int)y1, (int)(x2 - x1), (int)(y2 - y1), 0xFF00FF88);
            graphics.pose().popPose();
        }

        renderParticles(graphics, deltaTime);

        graphics.pose().popPose();

        if (screenAnimation < 1.0f) {
            int overlayAlpha = (int)((1.0f - screenAnimation) * 255);
            graphics.fill(0, 0, width, height, (overlayAlpha << 24) | 0x121212);
        }

        if (isSearching) {
            renderSearchMenu(graphics);
        }
        if (activeItemPicker != null) {
            renderItemPickerOverlay(graphics);
        }
    }

    private void renderSearchMenu(GuiGraphics graphics) {
        int mw = 140;
        int itemH = 12;
        int maxVisible = 12;
        int visibleCount = Math.min(maxVisible, filteredTypes.size());
        int mh = 15 + visibleCount * itemH;
        
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 5000);
        graphics.fill(menuX + 2, menuY + 2, menuX + mw + 2, menuY + mh + 2, 0x55000000);
        graphics.fill(menuX, menuY, menuX + mw, menuY + mh, 0xEE1A1A1A);
        graphics.renderOutline(menuX, menuY, mw, mh, 0xFF00FF88);
        graphics.drawString(font, "> " + searchQuery + "_", menuX + 4, menuY + 4, 0xFF00FF88, false);
        
        for (int i = 0; i < visibleCount; i++) {
            int idx = i + searchScrollOffset;
            if (idx >= filteredTypes.size()) break;
            
            net.minecraft.resources.ResourceLocation type = filteredTypes.get(idx);
            // Try to get a friendly name
            String displayName = type.getPath().replace("_", " ");
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            
            boolean hovered = mouseY >= menuY + 15 + i * itemH && mouseY < menuY + 15 + (i + 1) * itemH && mouseX >= menuX && mouseX <= menuX + mw;
            int color = (idx == 0 || hovered) ? 0xFFFFFFFF : 0xFF888888;
            if (idx == 0 || hovered) {
                graphics.fill(menuX + 1, menuY + 15 + i * itemH, menuX + mw - 1, menuY + 15 + (i + 1) * itemH, 0x4400FF88);
            }
            graphics.drawString(font, displayName, menuX + 6, menuY + 16 + i * itemH, color, false);
        }
        
        // Scroll indicator
        if (filteredTypes.size() > maxVisible) {
            float progress = (float) searchScrollOffset / (filteredTypes.size() - maxVisible);
            int barY = menuY + 15 + (int)(progress * (mh - 25));
            graphics.fill(menuX + mw - 3, menuY + 15, menuX + mw - 1, menuY + mh - 1, 0x22FFFFFF);
            graphics.fill(menuX + mw - 3, barY, menuX + mw - 1, barY + 10, 0xFF00FF88);
        }
        
        graphics.pose().popPose();
    }

    private void drawGrid(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(panX, panY, 0);
        int gridSize = 20;
        int startX = (int)(-panX - (width / 2f) / zoom - 20);
        int startY = (int)(-panY - (height / 2f) / zoom - 20);
        int endX = (int)(-panX + (width / 2f) / zoom + width + 20);
        int endY = (int)(-panY + (height / 2f) / zoom + height + 20);
        startX = (startX / gridSize) * gridSize;
        startY = (startY / gridSize) * gridSize;
        for (int i = startX; i < endX; i += gridSize) graphics.fill(i, startY, i + 1, endY, 0x12FFFFFF);
        for (int i = startY; i < endY; i += gridSize) graphics.fill(startX, i, endX, i + 1, 0x12FFFFFF);
        graphics.pose().popPose();
    }

    private void drawConnection(GuiGraphics graphics, WConnection conn, float partialTick) {
        WNode src = findNode(conn.sourceNode());
        WNode tgt = findNode(conn.targetNode());
        if (src == null || tgt == null) return;
        int x1 = src.getX() + src.getWidth();
        int y1 = src.getY() + 18 + conn.sourcePin() * 12;
        int x2 = tgt.getX();
        int y2 = tgt.getY() + 18 + conn.targetPin() * 12;
        drawSmoothCurve(graphics, x1, y1, x2, y2, 0xAA00FF88, 1.5f);
        float speed = 0.003f;
        float time = (System.currentTimeMillis() % 1000000) * speed; 
        float cycle = 4.0f; 
        float t = (time - src.getTopoDepth()) % cycle;
        if (t < 0) t += cycle;
        if (t >= 0 && t <= 1.0f) {
            float pulsePos = t * t * (3.0f - 2.0f * t);
            float dist = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);
            drawPulsePoint(graphics, x1, y1, x2, y2, pulsePos, dist);
            graphics.pose().popPose();
        }
    }

    private void drawPulsePoint(GuiGraphics graphics, int x1, int y1, int x2, int y2, float t, float dist) {
        float cx1 = x1 + (x2 - x1) * 0.5f; float cy1 = y1;
        float cx2 = x1 + (x2 - x1) * 0.5f; float cy2 = y2;
        int segments = (int) Math.max(12, dist / 6);
        float step = 0.25f / segments; 
        for (int i = 0; i < segments; i++) {
            float segmentT = t - (i * step); if (segmentT < 0) continue;
            float mt = 1.0f - segmentT;
            float x = mt * mt * mt * x1 + 3 * mt * mt * segmentT * cx1 + 3 * mt * segmentT * segmentT * cx2 + segmentT * segmentT * segmentT * x2;
            float y = mt * mt * mt * y1 + 3 * mt * mt * segmentT * cy1 + 3 * mt * segmentT * segmentT * cy2 + segmentT * segmentT * segmentT * y2;
            int alpha = (int)(255 * (1.0f - (float)i / segments));
            int color = (alpha << 24) | 0x00FF88;
            graphics.fill((int)x - 3, (int)y - 3, (int)x + 3, (int)y + 3, (color & 0x00FFFFFF) | ((alpha / 5) << 24));
            graphics.fill((int)x - 1, (int)y - 1, (int)x + 1, (int)y + 1, (color & 0x00FFFFFF) | (alpha << 24));
        }
    }

    private void drawSmoothCurve(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, float thickness) {
        int steps = 24; int lastX = x1; int lastY = y1;
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            float cx1 = x1 + (x2 - x1) * 0.5f; float cy1 = y1;
            float cx2 = x1 + (x2 - x1) * 0.5f; float cy2 = y2;
            float mt = 1.0f - t;
            float x = mt * mt * mt * x1 + 3 * mt * mt * t * cx1 + 3 * mt * t * t * cx2 + t * t * t * x2;
            float y = mt * mt * mt * y1 + 3 * mt * mt * t * cy1 + 3 * mt * t * t * cy2 + t * t * t * y2;
            drawLine(graphics, lastX, lastY, (int)x, (int)y, color, thickness);
            lastX = (int)x; lastY = (int)y;
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, float thickness) {
        int dx = x2 - x1; int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy)); if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int px = x1 + (int)(dx * t); int py = y1 + (int)(dy * t);
            graphics.fill(px, py, px + (int)Math.max(1, thickness), py + (int)Math.max(1, thickness), color);
        }
    }

    private void renderParticles(GuiGraphics graphics, float deltaTime) {
        for (int i = editorParticles.size() - 1; i >= 0; i--) {
            NodeParticle p = editorParticles.get(i);
            p.x += p.vx * deltaTime * 60.0; p.y += p.vy * deltaTime * 60.0; p.life -= deltaTime * 60.0;
            if (p.life <= 0) { editorParticles.remove(i); continue; }
            float alpha = (float) p.life / p.maxLife;
            int rColor = (p.color & 0xFFFFFF) | ((int)(alpha * 255) << 24);
            graphics.fill((int)p.x, (int)p.y, (int)p.x + 2, (int)p.y + 2, rColor);
        }
    }

    private WNode findNode(UUID id) {
        return graph.getNodes().stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeItemPicker != null) {
            if (handleItemPickerClick(mouseX, mouseY, button)) return true;
            activeItemPicker = null;
            return true;
        }

        int nx = (int)((mouseX - width / 2f) / zoom + width / 2f - panX);
        int ny = (int)((mouseY - height / 2f) / zoom + height / 2f - panY);
        if (isSearching) {
            int itemH = 12;
            int mw = 140;
            int maxVisible = 12;
            int visibleCount = Math.min(maxVisible, filteredTypes.size());
            if (mouseX >= menuX && mouseX <= menuX + mw && mouseY >= menuY + 15 && mouseY <= menuY + 15 + visibleCount * itemH) {
                int index = (int)((mouseY - (menuY + 15)) / itemH) + searchScrollOffset;
                if (index >= 0 && index < filteredTypes.size()) {
                    addNodeAt(filteredTypes.get(index), nx, ny); isSearching = false; return true;
                }
            }
            isSearching = false; return true;
        }
        if (button == 1) {
            boolean hitAnything = false;
            for (WNode node : graph.getNodes()) {
                if (node.getPinAt(nx - node.getX(), ny - node.getY(), true) != -1 || node.getPinAt(nx - node.getX(), ny - node.getY(), false) != -1 || (nx >= node.getX() && nx <= node.getX() + node.getWidth() && ny >= node.getY() && ny <= node.getY() + node.getHeight())) {
                    hitAnything = true; break;
                }
            }
            if (!hitAnything) { isSearching = true; searchQuery = ""; menuX = (int)mouseX; menuY = (int)mouseY; updateSearch(); return true; }
        }
        if (button == 1) {
            for (WNode node : graph.getNodes()) {
                int inPin = node.getPinAt(nx - node.getX(), ny - node.getY(), true);
                int outPin = node.getPinAt(nx - node.getX(), ny - node.getY(), false);
                if (inPin != -1) { graph.getConnections().removeIf(c -> c.targetNode().equals(node.getId()) && c.targetPin() == inPin); graph.updateTopology(); return true; }
                if (outPin != -1) { graph.getConnections().removeIf(c -> c.sourceNode().equals(node.getId()) && c.sourcePin() == outPin); graph.updateTopology(); return true; }
            }
        }
        if (button == 2 || (button == 1 && Screen.hasShiftDown())) { isPanning = true; return true; }
        for (int i = graph.getNodes().size() - 1; i >= 0; i--) {
            WNode node = graph.getNodes().get(i);
            int outPin = node.getPinAt(nx - node.getX(), ny - node.getY(), false);
            if (outPin != -1) { linkingNode = node; linkingPin = outPin; return true; }
            if (nx >= node.getX() && nx <= node.getX() + node.getWidth() && ny >= node.getY() && ny <= node.getY() + node.getHeight()) {
                if (!Screen.hasShiftDown() && !node.isSelected()) graph.getNodes().forEach(n -> n.setSelected(false));
                node.setSelected(true);
                selectedNode = node; // Set before element interaction!

                if (node.mouseClicked(nx - node.getX(), ny - node.getY(), button)) return true;
                
                draggingNode = node; dragOffsetX = nx - node.getX(); dragOffsetY = ny - node.getY();
                graph.getNodes().remove(i); graph.getNodes().add(node); return true;
            }
        }
        if (button == 0) {
            isSelecting = true; selStartX = nx; selStartY = ny; selEndX = nx; selEndY = ny;
            if (!Screen.hasShiftDown()) graph.getNodes().forEach(n -> n.setSelected(false));
            return true;
        }
        selectedNode = null; if (!Screen.hasShiftDown()) graph.getNodes().forEach(n -> n.setSelected(false));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int nx = (int)((mouseX - width / 2f) / zoom + width / 2f - panX);
        int ny = (int)((mouseY - height / 2f) / zoom + height / 2f - panY);
        if (isSelecting) {
            float x1 = (float)Math.min(selStartX, selEndX); float y1 = (float)Math.min(selStartY, selEndY);
            float x2 = (float)Math.max(selStartX, selEndX); float y2 = (float)Math.max(selStartY, selEndY);
            for (WNode node : graph.getNodes()) {
                if (node.getX() + node.getWidth() >= x1 && node.getX() <= x2 && node.getY() + node.getHeight() >= y1 && node.getY() <= y2) node.setSelected(true);
            }
            isSelecting = false; return true;
        }
        if (linkingNode != null) {
            for (WNode node : graph.getNodes()) {
                int inPin = node.getPinAt(nx - node.getX(), ny - node.getY(), true);
                if (inPin != -1) { graph.connect(linkingNode.getId(), linkingPin, node.getId(), inPin); break; }
            }
        }
        if (selectedNode != null) selectedNode.mouseReleased(nx, ny, button);
        isPanning = false; draggingNode = null; linkingNode = null; linkingPin = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isSelecting) {
            float mx = (float)((mouseX - width / 2f) / zoom + width / 2f - panX); float my = (float)((mouseY - height / 2f) / zoom + height / 2f - panY);
            selEndX = mx; selEndY = my; return true;
        }
        if (isPanning) { panX += dragX / zoom; panY += dragY / zoom; return true; }
        if (draggingNode != null) {
            double dx = dragX / zoom; double dy = dragY / zoom;
            for (WNode n : graph.getNodes()) if (n.isSelected()) n.setPos(n.getX() + (int)dx, n.getY() + (int)dy);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeItemPicker != null) {
            if (scrollY > 0 && itemScrollOffset > 0) itemScrollOffset--;
            if (scrollY < 0 && (itemScrollOffset + 1) * 8 < filteredItems.size()) itemScrollOffset++;
            return true;
        }
        if (isSearching) {
            if (scrollY > 0 && searchScrollOffset > 0) searchScrollOffset--;
            if (scrollY < 0 && searchScrollOffset + 12 < filteredTypes.size()) searchScrollOffset++;
            return true;
        }
        zoom = (float) Math.max(0.1, Math.min(3.0, zoom + scrollY * 0.1)); return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeItemPicker != null) {
            if (keyCode == 256) { activeItemPicker = null; return true; } // ESC
            if (keyCode == 259) { // BACKSPACE
                if (!itemSearchQuery.isEmpty()) {
                    itemSearchQuery = itemSearchQuery.substring(0, itemSearchQuery.length() - 1);
                    updateItemSearch();
                }
                return true;
            }
            return true;
        }
        if (isSearching) {
            if (keyCode == 256) { isSearching = false; return true; }
            if (keyCode == 257 || keyCode == 335) { if (!filteredTypes.isEmpty()) addNodeAt(filteredTypes.get(0), (int)(menuX - panX), (int)(menuY - panY)); isSearching = false; return true; }
            if (keyCode == 259) { if (!searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); updateSearch(); } return true; }
            return true;
        }
        if (selectedNode != null && (keyCode == 261 || keyCode == 88)) { graph.removeNode(selectedNode); selectedNode = null; return true; }
        if (keyCode == 73 && hasControlDown() && hasAltDown()) { spawnSecretNode(); return true; }
        if (keyCode == 65 && hasControlDown()) { graph.getNodes().forEach(n -> n.setSelected(true)); return true; }
        if (keyCode == 67 && hasControlDown()) { copySelected(); return true; }
        if (keyCode == 86 && hasControlDown()) { pasteFromClipboard(); return true; }
        if (keyCode == 65 && Screen.hasShiftDown()) {
            isSearching = true;
            searchQuery = "";
            menuX = this.mouseX;
            menuY = this.mouseY;
            updateSearch(); return true;
        }
        if (selectedNode != null && selectedNode.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeItemPicker != null) {
            itemSearchQuery += codePoint;
            updateItemSearch();
            return true;
        }
        if (isSearching) {
            if (searchQuery.isEmpty() && (codePoint == 'a' || codePoint == 'A' || codePoint == 'ф' || codePoint == 'Ф')) return true;
            searchQuery += codePoint; updateSearch(); return true;
        }
        if (selectedNode != null && selectedNode.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    public void openItemPicker(dev.devce.websnodelib.api.elements.WItemPicker picker, int mx, int my) {
        this.activeItemPicker = picker;
        this.itemMenuX = mx;
        this.itemMenuY = my;
        this.itemSearchQuery = "";
        this.itemScrollOffset = 0;
        updateItemSearch();
    }

    private void updateItemSearch() {
        filteredItems = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
            .filter(item -> item.getDescriptionId().toLowerCase().contains(itemSearchQuery.toLowerCase()) || 
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString().contains(itemSearchQuery.toLowerCase()))
            .collect(java.util.stream.Collectors.toList());
    }

    private void renderItemPickerOverlay(GuiGraphics graphics) {
        int w = 200;
        int h = 160;
        int x = itemMenuX;
        int y = itemMenuY;
        if (x + w > width) x = width - w - 10;
        if (y + h > height) y = height - h - 10;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 8000);
        graphics.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
        graphics.fill(x, y, x + w, y + h, 0xEE1A1A1A);
        graphics.renderOutline(x, y, w, h, 0xFF00FF88);
        
        graphics.fill(x + 5, y + 5, x + w - 5, y + 20, 0xFF000000);
        graphics.drawString(font, "> " + itemSearchQuery + "_", x + 10, y + 8, 0xFF00FF88, false);

        int cols = 8;
        int rows = 6;
        int startIdx = itemScrollOffset * cols;
        for (int i = 0; i < cols * rows; i++) {
            int idx = startIdx + i;
            if (idx >= filteredItems.size()) break;
            int ix = x + 10 + (i % cols) * 22;
            int iy = y + 25 + (i / cols) * 22;
            boolean hovered = mouseX >= ix && mouseX <= ix + 20 && mouseY >= iy && mouseY <= iy + 20;
            graphics.fill(ix, iy, ix + 20, iy + 20, hovered ? 0x44FFFFFF : 0x22000000);
            graphics.renderFakeItem(new net.minecraft.world.item.ItemStack(filteredItems.get(idx)), ix + 2, iy + 2);
        }
        graphics.pose().popPose();
    }

    private boolean handleItemPickerClick(double mx, double my, int button) {
        int w = 200;
        int h = 160;
        int x = itemMenuX;
        int y = itemMenuY;
        if (x + w > width) x = width - w - 10;
        if (y + h > height) y = height - h - 10;

        int cols = 8;
        int rows = 6;
        int startIdx = itemScrollOffset * cols;
        for (int i = 0; i < cols * rows; i++) {
            int idx = startIdx + i;
            if (idx >= filteredItems.size()) break;
            int ix = x + 10 + (i % cols) * 22;
            int iy = y + 25 + (i / cols) * 22;
            if (mx >= ix && mx <= ix + 20 && my >= iy && my <= iy + 20) {
                activeItemPicker.setStack(new net.minecraft.world.item.ItemStack(filteredItems.get(idx)));
                activeItemPicker = null;
                return true;
            }
        }
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void addNodeAt(net.minecraft.resources.ResourceLocation type, int x, int y) {
        WNode node = dev.devce.websnodelib.api.NodeRegistry.createNode(type, x, y);
        if (node != null) graph.addNode(node);
    }

    private void spawnSecretNode() {
        int nx = (int)(-panX + width / 2f); int ny = (int)(-panY + height / 2f);
        WNode node = new WNode(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("websnodelib", "secret"), "Webyep's Gift", nx, ny);
        node.setWidth(160);
        node.addElement(new dev.devce.websnodelib.api.elements.WGif(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("websnodelib", "textures/gui/secret.gif"), 150, 150));
        node.addElement(new dev.devce.websnodelib.api.elements.WLabel("   Made with love from Webyep", 0xFF00FF88));
        graph.addNode(node);
    }

    private void updateSearch() {
        filteredTypes.clear();
        String query = searchQuery.toLowerCase();
        List<ResourceLocation> all = new ArrayList<>(dev.devce.websnodelib.api.NodeRegistry.getRegisteredTypes());
        all.sort((a, b) -> a.getPath().compareTo(b.getPath()));
        
        java.util.Set<String> foundTypes = new java.util.HashSet<>();
        if (graph.getContext() instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity sputnik) {
            for (dev.devce.rocketnautics.api.peripherals.IPeripheral p : sputnik.getPeripherals()) {
                if (p != null && p.getPeripheralType() != null) {
                    foundTypes.add(p.getPeripheralType().toLowerCase());
                }
            }
        }
        
        for (net.minecraft.resources.ResourceLocation type : all) {
            String path = type.getPath().toLowerCase();
            
            // Context-sensitive filtering for peripheral controls
            if (path.equals("vector_control") && !foundTypes.contains("vector_engine")) continue;
            if (path.equals("rcs_control") && !foundTypes.contains("rcs")) continue;
            if (path.equals("booster_control") && !foundTypes.contains("booster")) continue;
            if (path.equals("thruster_control") && !foundTypes.contains("thruster")) continue;
            
            if (query.isEmpty() || path.contains(query)) {
                filteredTypes.add(type);
            }
        }
        searchScrollOffset = 0;
    }

    private void copySelected() {
        ListTag nodesTag = new ListTag();
        for (WNode node : graph.getNodes()) if (node.isSelected()) nodesTag.add(node.save());
        if (nodesTag.isEmpty()) return;
        CompoundTag root = new CompoundTag(); root.put("nodes", nodesTag);
        ListTag connTag = new ListTag();
        for (WConnection conn : graph.getConnections()) {
            WNode src = findNode(conn.sourceNode()); WNode tgt = findNode(conn.targetNode());
            if (src != null && tgt != null && src.isSelected() && tgt.isSelected()) {
                CompoundTag c = new CompoundTag();
                c.putString("src", conn.sourceNode().toString()); c.putInt("srcP", conn.sourcePin());
                c.putString("tgt", conn.targetNode().toString()); c.putInt("tgtP", conn.targetPin());
                connTag.add(c);
            }
        }
        root.put("conns", connTag);
        minecraft.keyboardHandler.setClipboard(Base64.getEncoder().encodeToString(root.toString().getBytes()));
    }

    private void pasteFromClipboard() {
        String data = minecraft.keyboardHandler.getClipboard(); if (data == null || data.isEmpty()) return;
        try {
            String decoded = new String(Base64.getDecoder().decode(data));
            CompoundTag root = TagParser.parseTag(decoded);
            ListTag nodesTag = root.getList("nodes", 10);
            Map<UUID, UUID> oldToNew = new HashMap<>();
            graph.getNodes().forEach(n -> n.setSelected(false));
            for (int i = 0; i < nodesTag.size(); i++) {
                CompoundTag nTag = nodesTag.getCompound(i);
                net.minecraft.resources.ResourceLocation type = net.minecraft.resources.ResourceLocation.parse(nTag.getString("typeId"));
                WNode newNode = dev.devce.websnodelib.api.NodeRegistry.createNode(type, nTag.getInt("x") + 10, nTag.getInt("y") + 10);
                if (newNode != null) {
                    newNode.load(nTag); UUID oldId = UUID.fromString(nTag.getString("id"));
                    oldToNew.put(oldId, newNode.getId()); graph.addNode(newNode); newNode.setSelected(true);
                }
            }
            ListTag connTag = root.getList("conns", 10);
            for (int i = 0; i < connTag.size(); i++) {
                CompoundTag c = connTag.getCompound(i);
                UUID newSrc = oldToNew.get(UUID.fromString(c.getString("src")));
                UUID newTgt = oldToNew.get(UUID.fromString(c.getString("tgt")));
                if (newSrc != null && newTgt != null) graph.connect(newSrc, c.getInt("srcP"), newTgt, c.getInt("tgtP"));
            }
        } catch (Exception e) {}
    }
}
