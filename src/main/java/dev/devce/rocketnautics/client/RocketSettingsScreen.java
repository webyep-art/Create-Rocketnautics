package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class RocketSettingsScreen extends Screen {
    private final Screen lastScreen;
    private boolean isServerTab = false;
    private GuiGraphics guiGraphics;

    public RocketSettingsScreen(Screen lastScreen) {
        super(Component.translatable("gui.rocketnautics.settings.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int x = this.width / 2;
        int y = 40;

        
        this.addRenderableWidget(Button.builder(Component.literal("Client Settings"), b -> {
            this.isServerTab = false;
            this.rebuildWidgets();
        }).bounds(x - 110, y, 100, 20).build()).active = isServerTab;

        this.addRenderableWidget(Button.builder(Component.literal("Server Settings"), b -> {
            this.isServerTab = true;
            this.rebuildWidgets();
        }).bounds(x + 10, y, 100, 20).build()).active = !isServerTab;

        y += 30;

        if (isServerTab) {
            renderServerSettings(x, y);
        } else {
            renderClientSettings(x, y);
        }

        
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(x - 100, this.height - 30, 200, 20).build());
    }

    private void renderClientSettings(int x, int y) {
        
        this.addRenderableWidget(CycleButton.onOffBuilder(RocketConfig.CLIENT.showDebugOverlay.get())
            .create(x - 100, y, 200, 20, Component.literal("Debug Overlay"), (btn, val) -> {
                RocketConfig.CLIENT.showDebugOverlay.set(val);
                RocketConfig.CLIENT.showDebugOverlay.save();
            }));
        y += 25;

        
        this.addRenderableWidget(CycleButton.onOffBuilder(RocketConfig.CLIENT.enableDynamicRenderDistance.get())
            .create(x - 100, y, 200, 20, Component.literal("Dynamic Render Distance"), (btn, val) -> {
                RocketConfig.CLIENT.enableDynamicRenderDistance.set(val);
                RocketConfig.CLIENT.enableDynamicRenderDistance.save();
            }));
        y += 25;

        
        this.addRenderableWidget(CycleButton.builder(val -> Component.literal("Shake: " + val + "x"))
            .withValues(0.0, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0)
            .withInitialValue(RocketConfig.CLIENT.shakeIntensity.get())
            .create(x - 100, y, 200, 20, Component.literal("Camera Shake"), (btn, val) -> {
                RocketConfig.CLIENT.shakeIntensity.set((Double) val);
                RocketConfig.CLIENT.shakeIntensity.save();
            }));
    }

    private void renderServerSettings(int x, int y) {
        boolean isLocal = this.minecraft.getSingleplayerServer() != null;
        
        if (!isLocal) {
            y += 10;
            guiGraphics.drawCenteredString(this.font, Component.literal("Server-side parameters are locked on Multiplayer."), x, y, 0xFF5555);
            y += 15;
            guiGraphics.drawCenteredString(this.font, Component.literal("Only the server administrator can change these"), x, y, 0xAAAAAA);
            y += 10;
            guiGraphics.drawCenteredString(this.font, Component.literal("via the serverconfig folder."), x, y, 0xAAAAAA);
            return;
        }

        
        
        
        this.addRenderableWidget(CycleButton.builder(val -> Component.literal("Max Fuel: " + val + " mB/t"))
            .withValues(10, 20, 40, 60, 80, 100, 200)
            .withInitialValue(RocketConfig.SERVER.maxFuelConsumption.get())
            .create(x - 100, y, 200, 20, Component.literal("Max Fuel Consumption"), (btn, val) -> {
                RocketConfig.SERVER.maxFuelConsumption.set((Integer) val);
                RocketConfig.SERVER.maxFuelConsumption.save();
            }));
        y += 25;

        
        this.addRenderableWidget(CycleButton.builder(val -> Component.literal("Jetpack Power: " + val))
            .withValues(0.05, 0.1, 0.15, 0.2, 0.3, 0.5)
            .withInitialValue(RocketConfig.SERVER.jetpackThrust.get())
            .create(x - 100, y, 200, 20, Component.literal("Jetpack Power"), (btn, val) -> {
                RocketConfig.SERVER.jetpackThrust.set((Double) val);
                RocketConfig.SERVER.jetpackThrust.save();
            }));
        y += 25;

        
        this.addRenderableWidget(CycleButton.builder(val -> Component.literal("Sprint Power: " + val))
            .withValues(0.2, 0.3, 0.35, 0.4, 0.6, 1.0)
            .withInitialValue(RocketConfig.SERVER.jetpackSprintThrust.get())
            .create(x - 100, y, 200, 20, Component.literal("Jetpack Sprint Power"), (btn, val) -> {
                RocketConfig.SERVER.jetpackSprintThrust.set((Double) val);
                RocketConfig.SERVER.jetpackSprintThrust.save();
            }));
        y += 25;

        
        this.addRenderableWidget(CycleButton.builder(val -> Component.literal("Ignition Threshold: " + val + " mB/t"))
            .withValues(1, 2, 5, 10, 20)
            .withInitialValue(RocketConfig.SERVER.ignitionFlow.get())
            .create(x - 100, y, 200, 20, Component.literal("Ignition Flow"), (btn, val) -> {
                RocketConfig.SERVER.ignitionFlow.set((Integer) val);
                RocketConfig.SERVER.ignitionFlow.save();
            }));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.guiGraphics = guiGraphics; 
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        
        String subTitle = isServerTab ? "§eServer-side Physics & Logic" : "§bClient-side Visuals";
        guiGraphics.drawCenteredString(this.font, subTitle, this.width / 2, 75, 0xAAAAAA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
