package dev.devce.websnodelib.api.elements;

import dev.devce.websnodelib.api.WElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class WTextField extends WElement {
    private String value = "";
    private boolean focused = false;

    public WTextField(int width) {
        this.width = width;
        this.height = 12;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.renderOutline(x, y, width, height, focused ? 0xFF00FF88 : 0xFF888888);
        
        String display = value;
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) display += "_";
        graphics.drawString(Minecraft.getInstance().font, display, x + 2, y + 2, 0xFFFFFFFF);
    }

    @Override
    public boolean handleMouseClick(double localX, double localY, int button) {
        focused = localX >= 0 && localX <= width && localY >= 0 && localY <= height;
        return focused;
    }

    @Override
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        
        // AI FIX/ADD START
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            focused = false;
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!value.isEmpty()) {
                value = value.substring(0, value.length() - 1);
            }
            return true;
        }
        return true; // Consume other keys so they don't trigger global shortcuts
        /*
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !value.isEmpty()) {
            value = value.substring(0, value.length() - 1);
            return true;
        }
        return false;
        */
        // AI FIX/ADD STOP
    }

    @Override
    public boolean handleCharTyped(char codePoint, int modifiers) {
        if (!focused) return false;
        value += codePoint;
        return true;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    @Override
    public net.minecraft.nbt.CompoundTag save() {
        net.minecraft.nbt.CompoundTag tag = super.save();
        tag.putString("value", value);
        return tag;
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        this.value = tag.getString("value");
    }
}
