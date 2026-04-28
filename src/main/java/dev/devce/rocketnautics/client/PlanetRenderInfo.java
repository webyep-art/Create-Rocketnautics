package dev.devce.rocketnautics.client;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class PlanetRenderInfo {
    private final ResourceLocation texID;
    private final DynamicTexture texture;
    private int powerSize;
    private int centerX;
    private int centerZ;

    public PlanetRenderInfo(ResourceLocation texID, DynamicTexture texture) {
        this.texID = texID;
        this.texture = texture;
    }

    public ResourceLocation getTexID() {
        return texID;
    }

    public DynamicTexture getTexture() {
        return texture;
    }

    public int getPowerSize() {
        return powerSize;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public void setPowerSize(int powerSize) {
        this.powerSize = powerSize;
    }

    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    public void setCenterZ(int centerZ) {
        this.centerZ = centerZ;
    }
}
