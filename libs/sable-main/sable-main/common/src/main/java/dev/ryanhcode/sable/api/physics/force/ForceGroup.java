package dev.ryanhcode.sable.api.physics.force;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A grouping of forces, for queued force totals & display to the user for making contraptions
 *
 * @param name the name of the force group
 * @param description the description of the force group
 * @param color the RGB color of the force group
 * @param defaultDisplayed if the force group should be default displayed in GUIs
 */
public record ForceGroup(@NotNull Component name, @Nullable Component description, int color, boolean defaultDisplayed) {
}