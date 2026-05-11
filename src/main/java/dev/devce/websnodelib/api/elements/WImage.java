package dev.devce.websnodelib.api.elements;

import dev.devce.websnodelib.api.WElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class WImage extends WElement {
    private final ResourceLocation texture;
    private final int u, v;
    private final int texWidth, texHeight;

    public WImage(ResourceLocation texture, int width, int height) {
        this(texture, 0, 0, width, height, width, height);
    }

    public WImage(ResourceLocation texture, int u, int v, int width, int height, int texWidth, int texHeight) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        graphics.blit(texture, x, y, u, v, width, height, texWidth, texHeight);
    }
}
