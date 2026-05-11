package dev.devce.websnodelib.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import java.util.Base64;

public class WGraphSerializer {
    
    /**
     * Converts a graph to a Base64 encoded NBT string.
     */
    public static String serializeToBase64(WGraph graph) {
        CompoundTag tag = graph.save();
        return Base64.getEncoder().encodeToString(tag.toString().getBytes());
    }

    /**
     * Loads a graph from a Base64 encoded NBT string.
     */
    public static void deserializeFromBase64(WGraph graph, String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            CompoundTag tag = TagParser.parseTag(decoded);
            graph.load(tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
