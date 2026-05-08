package dev.devce.rocketnautics.content.orbit;

import dev.devce.rocketnautics.content.orbit.universe.DeepSpacePosition;
import dev.devce.rocketnautics.content.orbit.universe.UniverseDefinition;
import dev.devce.rocketnautics.network.DeepSpacePositionPayload;
import dev.devce.rocketnautics.network.UniverseDefinitionPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class DeepSpaceInstance {

    private final UniverseDefinition universe;
    private final int sideLength;
    private final int negXCorner;
    private final int negZCorner;
    private final AABB boundingBox;
    private final int id;

    private final DeepSpacePosition position = new DeepSpacePosition();

    public DeepSpaceInstance(DeepSpaceData manager, int sideLength, int negXCorner, int negZCorner, int id) {
        this.universe = manager.getUniverse();
        this.sideLength = sideLength;
        this.negXCorner = negXCorner;
        this.negZCorner = negZCorner;
        this.id = id;
        this.position.setLocalUniverseTicks(manager.getUniverseTicks());
        this.boundingBox = new AABB(negXCorner, 0, negZCorner, negXCorner + sideLength, sideLength, negZCorner + sideLength);
    }

    public DeepSpaceInstance(DeepSpaceData manager, CompoundTag tag) {
        this.universe = manager.getUniverse();
        this.sideLength = tag.getInt("SideLength");
        this.negXCorner = tag.getInt("NegX");
        this.negZCorner = tag.getInt("NegZ");
        this.id = tag.getInt("Id");
        this.position.setLocalUniverseTicks(tag.getLong("LocalTicks"));
        this.position.init(universe, tag.getString("Frame"), DeepSpaceHelper.read(DeepSpaceHelper.STAMPED_PVCOORDS_CODEC, tag.get("Coords")));
        this.boundingBox = new AABB(negXCorner, 0, negZCorner, negXCorner + sideLength, sideLength, negZCorner + sideLength);
    }

    // cannot be codec-driven due to the need for the DeepSpaceData object during deserialization.
    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SideLength", sideLength);
        tag.putInt("NegX", negXCorner);
        tag.putInt("NegZ", negZCorner);
        tag.putInt("Id", id);
        tag.putLong("LocalTicks", position.getLocalUniverseTicks());
        tag.putString("Frame", position.getCurrentOrbit().getFrame().getName());
        Tag c = DeepSpaceHelper.write(DeepSpaceHelper.STAMPED_PVCOORDS_CODEC, position.getCurrentOrbit().getPVCoordinates());
        if (c != null) tag.put("Coords", c);
        return tag;
    }

    public boolean isCorrupted() {
        return position.isCorrupted();
    }

    public int getId() {
        return id;
    }

    public int getSideLength() {
        return sideLength;
    }

    public int getNegXCorner() {
        return negXCorner;
    }

    public int getNegZCorner() {
        return negZCorner;
    }

    public DeepSpacePosition getPosition() {
        return position;
    }

    public void tick(MinecraftServer server) {
        // update position
        position.tick(universe);
        // handle contents
        if (server.getTickCount() % 20 == 0) {
            ServerLevel deepSpace = server.getLevel(DeepSpaceData.DEEP_SPACE_DIM);
            List<ServerPlayer> players = deepSpace.getPlayers(p -> boundingBox().contains(p.position()));
            for (ServerPlayer player : players) {
                // TODO only send the universe once
                PacketDistributor.sendToPlayer(player, new UniverseDefinitionPayload(universe));
                PacketDistributor.sendToPlayer(player, DeepSpacePositionPayload.of(position, universe));
            }
        }
    }

    public AABB boundingBox() {
        return boundingBox;
    }
}
