package dev.devce.rocketnautics.content.blocks.nodes;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class NodeConnection {
    public UUID sourceNode;
    public UUID targetNode;
    public int sourcePin; // 0 = first output pin
    public int targetPin; // 0 = first input pin

    public NodeConnection(UUID sourceNode, int sourcePin, UUID targetNode, int targetPin) {
        this.sourceNode = sourceNode;
        this.sourcePin = sourcePin;
        this.targetNode = targetNode;
        this.targetPin = targetPin;
    }

    public NodeConnection(CompoundTag tag) {
        this.sourceNode = tag.getUUID("SourceNode");
        this.targetNode = tag.getUUID("TargetNode");
        this.sourcePin = tag.getInt("SourcePin");
        this.targetPin = tag.getInt("TargetPin");
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("SourceNode", sourceNode);
        tag.putUUID("TargetNode", targetNode);
        tag.putInt("SourcePin", sourcePin);
        tag.putInt("TargetPin", targetPin);
        return tag;
    }
}
