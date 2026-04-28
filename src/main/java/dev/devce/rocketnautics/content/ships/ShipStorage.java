package dev.devce.rocketnautics.content.ships;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

public class ShipStorage {
    private static final Map<String, CompoundTag> SAVED_SHIPS = new HashMap<>();

    public static void saveShip(String name, CompoundTag tag) {
        SAVED_SHIPS.put(name, tag);
    }

    public static CompoundTag getShip(String name) {
        return SAVED_SHIPS.get(name);
    }

    public static boolean hasShip(String name) {
        return SAVED_SHIPS.containsKey(name);
    }
}
