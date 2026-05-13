package dev.devce.rocketnautics.content.orbit;

import dev.devce.rocketnautics.content.orbit.universe.DeepSpacePosition;
import dev.devce.rocketnautics.content.orbit.universe.UniverseDefinition;
import dev.devce.rocketnautics.network.DeepSpacePositionPayload;
import dev.devce.rocketnautics.network.UniverseDefinitionPayload;
import it.unimi.dsi.fastutil.doubles.DoubleObjectMutablePair;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeepSpaceInstance {

    private final DeepSpaceData manager;
    private final int sideLength;
    private final int negXCorner;
    private final int negZCorner;
    private final AABB boundingBox;
    private Vector3d center;
    private final long id;

    private final DeepSpacePosition position = new DeepSpacePosition();

    private final Map<UUID, DoubleObjectPair<Vector3d>> pendingPhysics = new Object2ObjectOpenHashMap<>();

    public DeepSpaceInstance(DeepSpaceData manager, int sideLength, int negXCorner, int negZCorner, long id) {
        this.manager = manager;
        this.sideLength = sideLength;
        this.negXCorner = negXCorner;
        this.negZCorner = negZCorner;
        this.id = id;
        this.position.setLocalUniverseTicks(manager.getUniverseTicks());
        this.boundingBox = new AABB(negXCorner, DeepSpaceData.LOGICAL_INSTANCE_HEIGHT, negZCorner, negXCorner + sideLength, DeepSpaceData.LOGICAL_INSTANCE_HEIGHT + sideLength, negZCorner + sideLength);
    }

    public DeepSpaceInstance(DeepSpaceData manager, CompoundTag tag) {
        this.manager = manager;
        this.sideLength = tag.getInt("SideLength");
        this.negXCorner = tag.getInt("NegX");
        this.negZCorner = tag.getInt("NegZ");
        this.id = tag.getLong("Id");
        this.position.setLocalUniverseTicks(tag.getLong("LocalTicks"));
        this.position.init(manager.getUniverse(), tag.getString("Frame"), DeepSpaceHelper.read(DeepSpaceHelper.STAMPED_PVCOORDS_CODEC, tag.get("Coords")));
        this.boundingBox = new AABB(negXCorner, DeepSpaceData.LOGICAL_INSTANCE_HEIGHT, negZCorner, negXCorner + sideLength, DeepSpaceData.LOGICAL_INSTANCE_HEIGHT + sideLength, negZCorner + sideLength);
    }

    // cannot be codec-driven due to the need for the DeepSpaceData object during deserialization.
    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SideLength", sideLength);
        tag.putInt("NegX", negXCorner);
        tag.putInt("NegZ", negZCorner);
        tag.putLong("Id", id);
        tag.putLong("LocalTicks", position.getLocalUniverseTicks());
        tag.putString("Frame", position.getCurrentOrbit().getFrame().getName());
        Tag c = DeepSpaceHelper.write(DeepSpaceHelper.STAMPED_PVCOORDS_CODEC, position.getCurrentOrbit().getPVCoordinates());
        if (c != null) tag.put("Coords", c);
        return tag;
    }

    public boolean isCorrupted() {
        return position.isCorrupted();
    }

    public long getId() {
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
        // handle physics
        if (!pendingPhysics.isEmpty()) {
            TimeStampedPVCoordinates coords = position.getCurrentPVCoords();
            Vector3d momentum = new Vector3d();
            double mass = 0;
            for (DoubleObjectPair<Vector3d> value : pendingPhysics.values()) {
                mass += value.firstDouble();
                value.right().mulAdd(value.firstDouble(), momentum, momentum);
            }
            pendingPhysics.clear();
            if (mass != 0) {
                Vector3D velocityChange = DeepSpaceHelper.adapt(momentum.div(mass));
                position.init(manager.getUniverse(), position.getFrame(),
                        new TimeStampedPVCoordinates(coords.getDate(), coords.getPosition(), coords.getVelocity().add(velocityChange)));
                manager.setDirty();
            }
        }
        // update position
        if (position.tick(manager.getUniverse())) {
            manager.setDirty();
        }
        // handle render data
        if (server.getTickCount() % 20 == 0) {
            ServerLevel deepSpace = server.getLevel(DeepSpaceData.DEEP_SPACE_DIM);
            List<ServerPlayer> players = deepSpace.getPlayers(p -> boundingBox().contains(p.position()));
            for (ServerPlayer player : players) {
                // TODO only send the universe only once
                PacketDistributor.sendToPlayer(player, new UniverseDefinitionPayload(manager.getUniverse()));
                PacketDistributor.sendToPlayer(player, DeepSpacePositionPayload.of(position, manager.getUniverse()));
            }
        }
    }

    public void applyVelocity(UUID id, Vector3dc velocity, double mass) {
        pendingPhysics.compute(id, (k, v) -> {
            if (v == null) {
                return DoubleObjectPair.of(mass, new Vector3d(velocity));
            }
            v.right().add(velocity);
            return v;
        });
    }

    public AABB boundingBox() {
        return boundingBox;
    }

    public Vector3dc getCenter() {
        if (center == null) {
            Vec3 v = boundingBox().getCenter();
            center = new Vector3d(v.x(), v.y(), v.z());
        }
        return center;
    }
}
