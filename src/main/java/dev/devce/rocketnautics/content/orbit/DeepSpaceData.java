package dev.devce.rocketnautics.content.orbit;

import dev.devce.rocketnautics.RocketNautics;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;

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
        getInstance(event.getServer()).tick();
    }

    // end static //

    private final UniverseDefinition universe = StandardUniverse.INSTANCE;

    private final Int2ObjectArrayMap<DeepSpaceInstance> instances = new Int2ObjectArrayMap<>();

    private long universeTicks;
    private int nextFreeID = 0;

    public void tick() {
        universeTicks += 1;
        instances.values().forEach(DeepSpaceInstance::tick);
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
}
