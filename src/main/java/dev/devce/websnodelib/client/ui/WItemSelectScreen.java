package dev.devce.websnodelib.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WItemSelectScreen extends Screen {
    private final Screen parent;
    private final Consumer<ItemStack> onSelect;
    private String searchQuery = "";
    private List<Item> filteredItems = new ArrayList<>();
    private int scrollOffset = 0;

    public WItemSelectScreen(Screen parent, Consumer<ItemStack> onSelect) {
        super(Component.literal("Select Item"));
        this.parent = parent;
        this.onSelect = onSelect;
        updateSearch();
    }

    private void updateSearch() {
        filteredItems = BuiltInRegistries.ITEM.stream()
            .filter(item -> item.getDescriptionId().toLowerCase().contains(searchQuery.toLowerCase()) || 
                            BuiltInRegistries.ITEM.getKey(item).toString().contains(searchQuery.toLowerCase()))
            .collect(Collectors.toList());
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        
        int w = 240;
        int h = 180;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        
        // Window
        g.fill(x, y, x + w, y + h, 0xEE1A1A1A);
        g.renderOutline(x, y, w, h, 0xFF00FF88);
        
        // Search Bar
        g.fill(x + 5, y + 5, x + w - 5, y + 20, 0xFF000000);
        g.renderOutline(x + 5, y + 5, w - 10, 15, 0xFF444444);
        g.drawString(font, "> " + searchQuery + (System.currentTimeMillis() % 1000 < 500 ? "_" : ""), x + 10, y + 8, 0xFF00FF88, false);
        
        // Item Grid
        int cols = 10;
        int rows = 7;
        int startIdx = scrollOffset * cols;
        
        for (int i = 0; i < cols * rows; i++) {
            int idx = startIdx + i;
            if (idx >= filteredItems.size()) break;
            
            Item item = filteredItems.get(idx);
            int ix = x + 10 + (i % cols) * 22;
            int iy = y + 25 + (i / cols) * 22;
            
            boolean hovered = mx >= ix && mx <= ix + 20 && my >= iy && my <= iy + 20;
            g.fill(ix, iy, ix + 20, iy + 20, hovered ? 0x44FFFFFF : 0x22000000);
            g.renderFakeItem(new ItemStack(item), ix + 2, iy + 2);
            
            if (hovered) {
                g.renderTooltip(font, new ItemStack(item), mx, my);
            }
        }
        
        // Footer info
        g.drawString(font, "Items found: " + filteredItems.size(), x + 5, y + h - 12, 0x88AAAAAA, false);
        g.drawString(font, "ESC to cancel", x + w - 70, y + h - 12, 0x88AAAAAA, false);
        
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int w = 240;
        int h = 180;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        
        int cols = 10;
        int rows = 7;
        int startIdx = scrollOffset * cols;
        
        for (int i = 0; i < cols * rows; i++) {
            int idx = startIdx + i;
            if (idx >= filteredItems.size()) break;
            
            int ix = x + 10 + (i % cols) * 22;
            int iy = y + 25 + (i / cols) * 22;
            
            if (mx >= ix && mx <= ix + 20 && my >= iy && my <= iy + 20) {
                onSelect.accept(new ItemStack(filteredItems.get(idx)));
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
        }
        
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (dy > 0 && scrollOffset > 0) scrollOffset--;
        if (dy < 0 && (scrollOffset + 1) * 10 < filteredItems.size()) scrollOffset++;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) { // ESC
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        if (key == 259) { // BACKSPACE
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                updateSearch();
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char code, int mods) {
        searchQuery += code;
        updateSearch();
        return true;
    }

    @Override
    public void tick() {
    }
}
