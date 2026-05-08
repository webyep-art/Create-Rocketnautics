package dev.devce.rocketnautics.content.blocks.nodes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import dev.devce.rocketnautics.api.nodes.NodeRegistry;
import dev.devce.rocketnautics.api.nodes.NodeHandler;

public class Node {
    public final UUID id;
    public ResourceLocation typeId;
    public int x, y;
    
    // Generic data for nodes
    public CompoundTag customData = new CompoundTag();

    // Legacy fields for compatibility or common use (can be moved to customData eventually)
    public double value = 0.0;
    public String operation = ">"; 
    public String selectedSide = "north"; 
    public ItemStack freqStack1 = ItemStack.EMPTY;
    public ItemStack freqStack2 = ItemStack.EMPTY;
    public String commentText = "New Comment";
    public int engineIndex = 0;
    
    // Transient fields for rich UI (not saved to NBT)
    public double lastGimbalX = 0;
    public double lastGimbalZ = 0;
    public double lastPitch = 0;
    public double lastYaw = 0;
    public double lastRoll = 0;
    public double[] history = new double[40];
    public int historyIndex = 0;
    public long lastTickTime = 0;

    public Node(ResourceLocation typeId, int x, int y) {
        this.id = UUID.randomUUID();
        this.typeId = typeId;
        this.x = x;
        this.y = y;
    }

    public Node(CompoundTag tag, HolderLookup.Provider registries) {
        this.id = tag.getUUID("Id");
        this.typeId = ResourceLocation.parse(tag.getString("Type"));
        this.x = tag.getInt("X");
        this.y = tag.getInt("Y");
        this.value = tag.getDouble("Value");
        this.operation = tag.getString("Operation");
        this.selectedSide = tag.getString("SelectedSide");
        this.commentText = tag.getString("CommentText");
        this.engineIndex = tag.getInt("EngineIndex");
        
        if (tag.contains("Data")) {
            this.customData = tag.getCompound("Data");
        }

        if (tag.contains("Freq1")) {
            this.freqStack1 = ItemStack.parseOptional(registries, tag.getCompound("Freq1"));
        }
        if (tag.contains("Freq2")) {
            this.freqStack2 = ItemStack.parseOptional(registries, tag.getCompound("Freq2"));
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Type", typeId.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putDouble("Value", value);
        tag.putString("Operation", operation);
        tag.putString("SelectedSide", selectedSide);
        tag.putString("CommentText", commentText);
        tag.putInt("EngineIndex", engineIndex);
        tag.put("Data", customData);
        
        if (!freqStack1.isEmpty()) {
            tag.put("Freq1", freqStack1.save(registries));
        }
        if (!freqStack2.isEmpty()) {
            tag.put("Freq2", freqStack2.save(registries));
        }
        return tag;
    }

    public NodeHandler getHandler() {
        return NodeRegistry.REGISTRY.get(typeId);
    }
}
