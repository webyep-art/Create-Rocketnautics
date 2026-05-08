package dev.devce.rocketnautics.api.nodes;

import dev.devce.rocketnautics.content.blocks.nodes.Node;
import net.minecraft.network.chat.Component;
import java.util.Collections;
import java.util.List;

public interface NodeHandler {
    Component getDisplayName();
    String getCategory();
    
    /**
     * Get the documentation description for this node.
     */
    default Component getDescription() { return Component.literal("No description available."); }
    
    /**
     * Evaluate the node and return its output value.
     */
    double evaluate(Node node, NodeContext context);

    /**
     * Number of input pins.
     */
    default int getInputCount() { return 0; }

    /**
     * Names of input pins for UI.
     */
    default List<Component> getInputNames() { return Collections.emptyList(); }

    /**
     * Names of output pins for UI.
     */
    default List<Component> getOutputNames() { return Collections.emptyList(); }

    /**
     * Color of the node in UI (ARGB).
     */
    default int getColor() { return 0xFF5D5D5D; }

    /**
     * Whether this node should be ticked even if its output is not connected.
     * Useful for nodes with side effects (like setters).
     */
    default boolean isTrigger() { return false; }

    /**
     * Icon for the node (e.g. "[H]")
     */
    default String getIcon() { return "[ ]"; }

    /**
     * Color of the header bar.
     */
    default int getHeaderColor() { return 0xFF00AAFF; }

    /**
     * Color of a specific input pin.
     */
    default int getPinColor(int pin) { return 0xFF00AAFF; }

    /**
     * Number of output pins.
     */
    default int getOutputCount() { return 1; }

    /**
     * Whether this node is available in the current context (e.g. if specific peripheral is present).
     */
    default boolean isAvailable(NodeContext context) { return true; }

    /**
     * Optional custom rendering inside the node body.
     * @return height used by the custom UI
     */
    default int renderCustomUI(net.minecraft.client.gui.GuiGraphics graphics, Node node, int x, int y, int width, double currentVal, float partialTick) {
        return 0;
    }
}
