package dev.devce.rocketnautics.content.orbit;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.orbit.universe.StandardUniverseProvider;
import dev.devce.rocketnautics.content.orbit.universe.UniverseDefinition;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

@EventBusSubscriber(modid = RocketNautics.MODID)
public class DeepSpaceData extends SavedData {
    // TODO move all dimension definitions to their own class (see SpaceTransitionHandler)
    public static final ResourceKey<Level> DEEP_SPACE_DIM = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "deep_space"));

    public static final String ID = "cosmonautics_deep_space_data";

    public static final AbsoluteDate EPOCH = AbsoluteDate.ARBITRARY_EPOCH;
    private static final long ATTOS_IN_TICK = 50000000000000000L;
    public static final TimeOffset TICK = new TimeOffset(0L, ATTOS_IN_TICK);

    public static DeepSpaceData getInstance(MinecraftServer server) {
        ServerLevel deepSpace = server.getLevel(DEEP_SPACE_DIM);
        DeepSpaceData data = deepSpace.getChunkSource().getDataStorage().computeIfAbsent(new Factory<>(DeepSpaceData::new, DeepSpaceData::load, null), ID);
        return data;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isDeepSpace() {
        return Minecraft.getInstance().level.dimension() == DEEP_SPACE_DIM;
    }

    public static boolean isDeepSpace(Level level) {
        return level.dimension() == DEEP_SPACE_DIM;
    }

    public static boolean isDeepSpace(ResourceKey<Level> key) {
        return key == DEEP_SPACE_DIM;
    }

    @SubscribeEvent
    public static void advanceUniverse(ServerTickEvent.Post event) {
        getInstance(event.getServer()).tick(event.getServer());
    }

    // end static //

    private final UniverseDefinition universe = StandardUniverseProvider.createSunOverworldMoon().build();

    private final Int2ObjectArrayMap<DeepSpaceInstance> instances = new Int2ObjectArrayMap<>();

    private long universeTicks;
    private int nextFreeID = 0;

    public void tick(MinecraftServer server) {
        universeTicks += 1;
        instances.values().forEach(i -> i.tick(server));
        if (instances.isEmpty()) debugInstance();
    }

    private void debugInstance() {
        // execute in rocketnautics:deep_space run tp Dev 48 16 16
        DeepSpaceInstance instance = new DeepSpaceInstance(this, 32, 32, 0, 0);
        instances.put(0, instance);
        instance.getPosition().init(universe, "overworld", new TimeStampedPVCoordinates(EPOCH, new Vector3D(0, 0, 6_000_000D), new Vector3D(0, 20_000, 0)));
    }

    public UniverseDefinition getUniverse() {
        return universe;
    }

    public long getUniverseTicks() {
        return universeTicks;
    }

    public AbsoluteDate getUniverseTime() {
        return EPOCH.shiftedBy(TICK.multiply(universeTicks));
    }

    public static AbsoluteDate getTime(long ticks) {
        if (ticks < 0) {
            return EPOCH.shiftedBy(TICK.negate().multiply(-ticks));
        }
        return EPOCH.shiftedBy(TICK.multiply(ticks));
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putLong("UniverseTicks", universeTicks);
        compoundTag.putInt("NextID", nextFreeID);
        ListTag list = new ListTag();
        for (DeepSpaceInstance instance : instances.values()) {
            list.add(instance.write());
        }
        compoundTag.put("Instances", list);
        return compoundTag;
    }

    private static DeepSpaceData load(CompoundTag tag, HolderLookup.Provider registries) {
        DeepSpaceData data = new DeepSpaceData();
        data.universeTicks = tag.getLong("UniverseTicks");
        data.nextFreeID = tag.getInt("NextID");
        ListTag list = tag.getList("Instances", ListTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            DeepSpaceInstance instance = new DeepSpaceInstance(data, list.getCompound(i));
            data.instances.put(instance.getId(), instance);
        }
        return data;
    }

    public DeepSpaceInstance getInstanceForPosition() {
        return null;
    }

    // TODO mixin to CollisionGetter#borderCollision and Entity#collectColliders to add this
    // collision box at the same place the world border's collision box is added.
    public static VoxelShape getColliderForPosition(Vec3 position) {
        // compute the instance we are in
        int[] sizeAndId = getChunkPowerSizeIdWithinSizeForParameters((int) position.x, (int) position.z);
        int[] corners = getCornerXCornerZForParameters(sizeAndId[0], sizeAndId[1]);
        int blockSize = 16 * (2 << sizeAndId[0]);
        // subtract the instance bounds from the infinity box
        return Shapes.join(
                Shapes.INFINITY,
                Shapes.box(
                        corners[0],
                        0,
                        corners[1],
                        corners[0] + blockSize + 1,
                        blockSize + 1,
                        corners[1] + blockSize + 1
                ),
                BooleanOp.ONLY_FIRST
        );
    }

    private static int[] getChunkPowerSizeIdWithinSizeForParameters(int negX, int negZ) {
        if (negX < 0 || negZ < 0) return new int[] { 1, 0 };
        // convert to chunkpos
        negX /= 16;
        negZ /= 16;
        // derive chunk size from X position
        // since the power term dominates at large scale, get a definite upper bound
        int chunkPowerSize = Math.max((int) (Math.log(negX * 1.1) / Math.log(2)) + 1, 1);
        // descend until we are below or equal to the target; at large scales, we will need to do this once.
        int size = chunkPowerSize * 16 + (2 << chunkPowerSize);
        while (size > negX) {
            chunkPowerSize--;
            size = chunkPowerSize * 16 + (2 << chunkPowerSize);
        }
        // derive id from Z position and chunk size
        return new int[] { chunkPowerSize, negZ / (16 + size) };
    }

    private static int[] getCornerXCornerZForParameters(int chunkPowerSize, int idWithinSize) {
        int chunkSize = 2 << chunkPowerSize;
        return new int[] { 16 * (chunkPowerSize * 16 + chunkSize), 16 * (idWithinSize * (16 + chunkSize)) };
    }
}
