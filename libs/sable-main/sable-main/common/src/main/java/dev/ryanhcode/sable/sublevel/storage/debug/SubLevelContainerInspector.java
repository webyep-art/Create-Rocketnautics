package dev.ryanhcode.sable.sublevel.storage.debug;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.api.client.editor.SingleWindowInspector;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.BitSet;

public class SubLevelContainerInspector extends SingleWindowInspector {
    public static final Component TITLE = Component.translatable("inspector.sable.sub_level_container.title");

    @Override
    protected void renderComponents() {

        final IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel clientLevel = minecraft.level;

        if (clientLevel != null) {

            final SubLevelContainer clientPlotContainer = ((SubLevelContainerHolder) clientLevel).sable$getPlotContainer();
            this.renderPlotContainer("Client", clientPlotContainer);

            if (singleplayerServer != null) {
                final ServerLevel serverLevel = singleplayerServer.getLevel(clientLevel.dimension());
                assert serverLevel != null : "Server level is null";

                ImGui.sameLine();

                final SubLevelContainer serverPlotContainer = ((SubLevelContainerHolder) serverLevel).sable$getPlotContainer();
                this.renderPlotContainer("Server", serverPlotContainer);
            }

        } else {
            ImGui.textDisabled("No level loaded");
        }
    }

    private void renderPlotContainer(final String name, final SubLevelContainer plotContainer) {
        final int sideLength = 1 << plotContainer.getLogSideLength();

        final float buttonStartX = ImGui.getCursorScreenPosX();
        final float buttonStartY = ImGui.getCursorScreenPosY();

        final float sizePixels = ImGui.getWindowHeight() - 40f;
        ImGui.button(name, sizePixels, sizePixels);

        final ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRect(buttonStartX, buttonStartY, buttonStartX + sizePixels, buttonStartY + sizePixels, 0xFFFFFFFF);

        final ImVec2 mousePos = ImGui.getMousePos();

        final float mouseX = mousePos.x - buttonStartX;
        final float mouseY = mousePos.y - buttonStartY;

        final int selectedXCell = (int) (mouseX / (sizePixels / sideLength));
        final int selectedYCell = (int) (mouseY / (sizePixels / sideLength));

        final boolean hovered = ImGui.isItemHovered();

        for (int x = 0; x < sideLength; x++) {
            for (int z = 0; z < sideLength; z++) {
                final BitSet occupancy = plotContainer.getOccupancy();
                final boolean isOccupied = occupancy.get(plotContainer.getIndex(x, z));
                final boolean hasSubLevel = plotContainer.getAllSubLevels().get(plotContainer.getIndex(x, z)) != null;

                final float xStart = buttonStartX + x * (sizePixels / sideLength);
                final float zStart = buttonStartY + z * (sizePixels / sideLength);

                final float xEnd = xStart + (sizePixels / sideLength);
                final float zEnd = zStart + (sizePixels / sideLength);

                final boolean cellSelected = hovered && x == selectedXCell && z == selectedYCell;

                final int occupiedColor = hasSubLevel ? 0xFF00FF00 : 0xFF006600;
                drawList.addRectFilled(xStart, zStart, xEnd, zEnd, isOccupied ? occupiedColor : 0xFF333333);
                drawList.addRect(xStart, zStart, xEnd, zEnd, 0xff444444);

                if (cellSelected) {
                    drawList.addRectFilled(xStart, zStart, xEnd, zEnd, 0xaa888888);
                }
            }
        }

        final SubLevel selectedSubLevel = plotContainer.getSubLevel(selectedXCell, selectedYCell);

        // The tooltip

        if (selectedSubLevel != null) {
            final int chunkCount = selectedSubLevel.getPlot().getLoadedChunks().size();
            ImGui.setTooltip(String.format("Loaded SubLevel\nChunks: %d", chunkCount));
        }

        // draw hovered coordinate
        if (hovered) {
            final float textY = buttonStartY + sizePixels - 20f;
            final String text = String.format("%d, %d", selectedXCell, selectedYCell);

            final float textWidth = ImGui.calcTextSize(text).x;
            final float textX = buttonStartX + sizePixels - textWidth - 5f;

            drawList.addText(textX, textY, 0xFFFFFFFF, text);
        }
        drawList.addText(buttonStartX + 5f, buttonStartY + 5f, 0xFFFFFFFF, name);
        drawList.addText(buttonStartX + 5f, buttonStartY + 25f, 0xFFFFFFFF, "%d loaded sub-level(s)".formatted(plotContainer.getLoadedCount()));

    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }
}
