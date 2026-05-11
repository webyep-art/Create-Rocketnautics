package dev.devce.websnodelib.api;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Represents a connection point on a node.
 * Pins can be of type INPUT or OUTPUT and carry a numeric (double) value.
 */
public class WPin extends WElement {
    /**
     * Defines the direction of data flow for the pin.
     */
    public enum Type { INPUT, OUTPUT }

    private final String name;
    private final Type type;
    private final int color;
    private boolean connected;
    private double value;

    /**
     * Creates a new pin.
     * @param name Display name of the pin.
     * @param type INPUT or OUTPUT.
     * @param color The accent color for the pin's icon.
     */
    public WPin(String name, Type type, int color) {
        this.name = name;
        this.type = type;
        this.color = color;
    }

    /**
     * Note: Pin rendering is currently managed by the parent WNode to ensure 
     * correct alignment with the node body and headers.
     */
    @Override
    public void render(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        // Rendering handled by WNode
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public int getColor() { return color; }

    /**
     * @return True if this pin is part of an active WConnection.
     */
    public boolean isConnected() { return connected; }

    /**
     * Updates the connection status of this pin.
     */
    public void setConnected(boolean connected) { this.connected = connected; }

    public net.minecraft.nbt.CompoundTag save() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putDouble("value", value);
        return tag;
    }

    public void load(net.minecraft.nbt.CompoundTag tag) {
        this.value = tag.getDouble("value");
    }

    /**
     * @return The current numeric value stored in this pin.
     */
    public double getValue() { return value; }

    /**
     * Sets the numeric value for this pin.
     * For INPUT pins, this is usually set by a WConnection.
     * For OUTPUT pins, this is set by the node's Evaluator.
     */
    public void setValue(double value) { this.value = value; }
}
