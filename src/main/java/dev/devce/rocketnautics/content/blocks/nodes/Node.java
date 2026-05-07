package dev.devce.rocketnautics.content.blocks.nodes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import java.util.UUID;

public class Node {
    public final UUID id;
    public NodeType type;
    public int x, y;
    
    // For NUMBER_INPUT or COMPARE type
    public double value = 0.0;
    public String operation = ">"; 
    public String selectedSide = "north"; 
    
    // For Wireless Link
    public ItemStack freqStack1 = ItemStack.EMPTY;
    public ItemStack freqStack2 = ItemStack.EMPTY;

    // For Comment
    public String commentText = "New Comment";

    // For Peripherals
    public int engineIndex = 0;

    public Node(NodeType type, int x, int y) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public Node(CompoundTag tag, HolderLookup.Provider registries) {
        this.id = tag.getUUID("Id");
        this.type = NodeType.valueOf(tag.getString("Type"));
        this.x = tag.getInt("X");
        this.y = tag.getInt("Y");
        this.value = tag.getDouble("Value");
        this.operation = tag.getString("Operation");
        this.selectedSide = tag.getString("SelectedSide");
        this.commentText = tag.getString("CommentText");
        this.engineIndex = tag.getInt("EngineIndex");
        
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
        tag.putString("Type", type.name());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putDouble("Value", value);
        tag.putString("Operation", operation);
        tag.putString("SelectedSide", selectedSide);
        tag.putString("CommentText", commentText);
        tag.putInt("EngineIndex", engineIndex);
        
        if (!freqStack1.isEmpty()) {
            tag.put("Freq1", freqStack1.save(registries));
        }
        if (!freqStack2.isEmpty()) {
            tag.put("Freq2", freqStack2.save(registries));
        }
        return tag;
    }
}
