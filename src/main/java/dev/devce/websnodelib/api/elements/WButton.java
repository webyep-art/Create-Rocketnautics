package dev.devce.websnodelib.api.elements;

import dev.devce.websnodelib.api.WElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class WButton extends WElement {
    private String label;
    private Runnable onClick;

    public WButton(String label, int width, Runnable onClick) {
        this.label = label;
        this.width = width;
        this.height = 14;
        this.onClick = onClick;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        // Background
        graphics.fill(x, y, x + width, y + height, hovered ? 0xFF444444 : 0xFF252525);
        graphics.renderOutline(x, y, width, height, hovered ? 0xFF00FF88 : 0xFF666666);
        
        // Label
        int textW = net.minecraft.client.Minecraft.getInstance().font.width(label);
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, label, x + (width - textW) / 2, y + 3, hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
    }

    @Override
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= 0 && mouseX <= width && mouseY >= 0 && mouseY <= height) {
            if (onClick != null) onClick.run();
            return true;
        }
        return false;
    }
}
