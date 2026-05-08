package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import dev.devce.rocketnautics.content.blocks.nodes.NodeGraph;
import dev.devce.rocketnautics.content.blocks.nodes.Node;
import dev.devce.rocketnautics.content.blocks.RocketThrusterBlockEntity;

import dev.devce.rocketnautics.api.nodes.NodeContext;
import dev.devce.rocketnautics.api.peripherals.IPeripheral;
import java.util.Collections;
import java.util.UUID;

public class SputnikBlockEntity extends BlockEntity implements NodeContext {
    public final NodeGraph graph = new NodeGraph();
    private boolean isForced = false;
    private ChunkPos lastForcedParentChunk = null;
    private ServerLevel lastForcedParentLevel = null;
    
    private final List<IPeripheral> discoveredPeripherals = new ArrayList<>();
    private int scanCooldown = 0;

    // Stage Planner Data
    // 5 Stages, each needs 2 items for frequency = 10 slots total
    public final ItemStackHandler stageFrequencies = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    // Conditions: 0 = Disabled, 1 = Altitude >, 2 = Altitude <, 3 = Velocity >, 4 = Velocity <, 5 = Time
    public final int[] stageConditions = new int[5];
    public final double[] stageValues = new double[5];

    // Redstone outputs per side: 0=down, 1=up, 2=north, 3=south, 4=west, 5=east
    private final int[] sideOutputs = new int[6];

    public SputnikBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.SPUTNIK.get(), pos, state);
    }

    private SubLevel getSubLevel() {
        if (level == null) return null;
        Object lvlObj = level;
        if (lvlObj instanceof SubLevel sl) return sl;
        Object obj = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, worldPosition);
        if (obj instanceof SubLevel sl) return sl;
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SputnikBlockEntity blockEntity) {
        if (blockEntity.scanCooldown-- <= 0) {
            blockEntity.refreshPeripherals();
            blockEntity.scanCooldown = 20;
        }

        if (!level.isClientSide) {
            if (!blockEntity.isForced) {
                blockEntity.forceChunk();
            }
            blockEntity.updateParentChunkLoading();
            blockEntity.tickNodes();
        }
    }

    private void tickNodes() {
        // Reset/Update peripherals if needed
        for (IPeripheral peripheral : discoveredPeripherals) {
            if (peripheral instanceof IThruster thruster && !thruster.isRemoved()) {
                thruster.setThrottle(1.0f);
                thruster.setActive(true);
                thruster.setGimbal(0, 0);
            }
        }

        graph.tick(this);
        
        // Sync graph values to client periodically for UI display
        if (level.getGameTime() % 5 == 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void refreshPeripherals() {
        discoveredPeripherals.clear();
        SubLevel sl = getSubLevel();
        
        if (sl != null) {
            net.minecraft.world.level.ChunkPos min = sl.getPlot().getChunkMin();
            net.minecraft.world.level.ChunkPos max = sl.getPlot().getChunkMax();

            for (int cx = min.x; cx <= max.x; cx++) {
                for (int cz = min.z; cz <= max.z; cz++) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof IPeripheral peripheral) {
                            discoveredPeripherals.add(peripheral);
                        }
                    }
                }
            }
        } else {
            for (int x = -8; x <= 8; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -8; z <= 8; z++) {
                        BlockEntity be = level.getBlockEntity(worldPosition.offset(x, y, z));
                        if (be instanceof IPeripheral peripheral && !discoveredPeripherals.contains(peripheral)) {
                            discoveredPeripherals.add(peripheral);
                        }
                    }
                }
            }
        }

        // Sort for stable IDs
        discoveredPeripherals.sort(Comparator.comparingInt((IPeripheral p) -> p.getBlockPos().getX())
            .thenComparingInt(p -> p.getBlockPos().getY())
            .thenComparingInt(p -> p.getBlockPos().getZ()));
    }

    @Override
    public List<IPeripheral> getPeripherals() {
        return Collections.unmodifiableList(discoveredPeripherals);
    }

    public int getEngineCount() { return discoveredPeripherals.size(); }
    public net.minecraft.core.BlockPos getEnginePos(int i) { 
        return (i >= 0 && i < discoveredPeripherals.size()) ? discoveredPeripherals.get(i).getBlockPos() : null; 
    }
    public double getEngineThrust(int i) {
        return (i >= 0 && i < discoveredPeripherals.size()) ? discoveredPeripherals.get(i).readValue("thrust") : 0;
    }
    public void refreshEngines() { refreshPeripherals(); }

    @Override
    public double evaluateInput(UUID nodeId, int pin) {
        return graph.getInputValue(nodeId, pin, this);
    }

    @Override
    public double getX() { return getGlobalPos().x; }
    @Override
    public double getY() { return getGlobalPos().y; }
    @Override
    public double getZ() { return getGlobalPos().z; }

    @Override
    public double getAltitude() {
        return getGlobalPos().y;
    }

    @Override
    public double getVelocity() {
        SubLevel subLevel = getSubLevel();
        if (subLevel != null) {
            var pose = subLevel.logicalPose();
            var lastPose = subLevel.lastPose();
            return new Vector3d(pose.position()).distance(lastPose.position()) * 20.0;
        }
        return 0;
    }

    @Override
    public double getPitch() {
        SubLevel subLevel = getSubLevel();
        if (subLevel != null) {
            Vector3d euler = subLevel.logicalPose().orientation().getEulerAnglesXYZ(new Vector3d());
            return Math.toDegrees(euler.x);
        }
        return 0;
    }

    @Override
    public net.minecraft.world.level.Level getLevel() {
        return level;
    }

    @Override
    public double getYaw() {
        SubLevel subLevel = getSubLevel();
        if (subLevel != null) {
            Vector3d euler = subLevel.logicalPose().orientation().getEulerAnglesXYZ(new Vector3d());
            return Math.toDegrees(euler.y);
        }
        return 0;
    }

    @Override
    public double getRoll() {
        SubLevel subLevel = getSubLevel();
        if (subLevel != null) {
            Vector3d euler = subLevel.logicalPose().orientation().getEulerAnglesXYZ(new Vector3d());
            return Math.toDegrees(euler.z);
        }
        return 0;
    }

    @Override
    public void setOutput(String side, int strength) {
        if (side.equalsIgnoreCase("all")) {
            for (int i = 0; i < 6; i++) {
                if (sideOutputs[i] != strength) {
                    sideOutputs[i] = strength;
                    if (level != null) {
                        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                    }
                }
            }
            return;
        }

        int sideIdx = switch (side.toLowerCase()) {
            case "down" -> 0;
            case "up" -> 1;
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> 2;
        };

        if (sideOutputs[sideIdx] != strength) {
            sideOutputs[sideIdx] = strength;
            if (level != null) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
        }
    }

    public int getSignal(net.minecraft.core.Direction direction) {
        return sideOutputs[direction.get3DDataValue()];
    }


    public void forceChunk() {
        if (level instanceof ServerLevel serverLevel) {
            ChunkPos chunkPos = new ChunkPos(worldPosition);
            serverLevel.setChunkForced(chunkPos.x, chunkPos.z, true);
            isForced = true;
        }
    }

    private void updateParentChunkLoading() {
        var subLevel = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, worldPosition);
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            ServerLevel parent = getParentLevel(serverSubLevel);
            if (parent != null) {
                Vector3d globalPos = serverSubLevel.logicalPose().position();
                ChunkPos currentParentChunk = new ChunkPos(BlockPos.containing(globalPos.x, globalPos.y, globalPos.z));
                
                if (lastForcedParentChunk == null || !lastForcedParentChunk.equals(currentParentChunk) || lastForcedParentLevel != parent) {
                    if (lastForcedParentChunk != null && lastForcedParentLevel != null) {
                        lastForcedParentLevel.setChunkForced(lastForcedParentChunk.x, lastForcedParentChunk.z, false);
                    }
                    
                    parent.setChunkForced(currentParentChunk.x, currentParentChunk.z, true);
                    lastForcedParentChunk = currentParentChunk;
                    lastForcedParentLevel = parent;
                }
            }
        }
    }

    public void releaseChunk() {
        if (level instanceof ServerLevel serverLevel && isForced) {
            ChunkPos chunkPos = new ChunkPos(worldPosition);
            serverLevel.setChunkForced(chunkPos.x, chunkPos.z, false);
            isForced = false;
        }
        if (lastForcedParentChunk != null && lastForcedParentLevel != null) {
            lastForcedParentLevel.setChunkForced(lastForcedParentChunk.x, lastForcedParentChunk.z, false);
            lastForcedParentChunk = null;
            lastForcedParentLevel = null;
        }
    }

    public int getBiomeColor() {
        if (level == null) return 0;
        Biome biome = level.getBiome(worldPosition).value();
        return biome.getFoliageColor();
    }

    public String getBiomeName() {
        if (level == null) return "Unknown";
        return level.getBiome(worldPosition).getRegisteredName();
    }

    public Vector3d getGlobalPos() {
        SubLevel subLevel = getSubLevel();
        if (subLevel != null) {
            return subLevel.logicalPose().position();
        }
        return new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
    }

    public Biome getGlobalBiome() {
        if (level == null) return null;
        var subLevel = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, worldPosition);
        
        if (subLevel != null && !level.isClientSide) {
            Vector3d global = getGlobalPos();
            ServerLevel parent = getParentLevel((ServerSubLevel) subLevel);
            if (parent != null) {
                return parent.getBiome(BlockPos.containing(global.x, 64, global.z)).value();
            }
        }
        
        return level.getBiome(worldPosition).value();
    }

    private ServerLevel getParentLevel(ServerSubLevel subLevel) {
        if (this.level == null || this.level.getServer() == null) return null;
        for (ServerLevel sl : this.level.getServer().getAllLevels()) {
            var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(sl);
            if (container != null) {
                for (var slItem : container.getAllSubLevels()) {
                    if (slItem.getUniqueId().equals(subLevel.getUniqueId())) {
                        return sl;
                    }
                }
            }
        }
        return null;
    }

    public int getGlobalBiomeColor() {
        Biome b = getGlobalBiome();
        return b != null ? b.getFoliageColor() : 0;
    }

    public String getGlobalBiomeName() {
        var subLevel = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, worldPosition);
        if (subLevel != null && !level.isClientSide) {
            ServerLevel parent = getParentLevel((ServerSubLevel) subLevel);
            if (parent != null) {
                Vector3d global = getGlobalPos();
                return parent.getBiome(BlockPos.containing(global.x, 64, global.z)).getRegisteredName();
            }
        }
        return getBiomeName();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Forced", isForced);
        tag.put("NodeGraph", graph.save(registries));
        tag.put("StageFrequencies", stageFrequencies.serializeNBT(registries));
        tag.putIntArray("StageConditions", stageConditions);
        
        CompoundTag valuesTag = new CompoundTag();
        for(int i=0; i<5; i++) {
            valuesTag.putDouble("v"+i, stageValues[i]);
        }
        tag.put("StageValues", valuesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isForced = tag.getBoolean("Forced");
        if (tag.contains("StageFrequencies")) {
            stageFrequencies.deserializeNBT(registries, tag.getCompound("StageFrequencies"));
        }
        if (tag.contains("StageConditions")) {
            int[] loaded = tag.getIntArray("StageConditions");
            System.arraycopy(loaded, 0, stageConditions, 0, Math.min(loaded.length, 5));
        }
        if (tag.contains("StageValues")) {
            CompoundTag valuesTag = tag.getCompound("StageValues");
            for(int i=0; i<5; i++) {
                stageValues[i] = valuesTag.getDouble("v"+i);
            }
        }
        if (tag.contains("NodeGraph")) {
            CompoundTag graphTag = tag.getCompound("NodeGraph");
            if (level != null && level.isClientSide && !graph.nodes.isEmpty()) {
                // Client-side periodic sync: update values ONLY to prevent race conditions with placement
                net.minecraft.nbt.ListTag nodesTag = graphTag.getList("Nodes", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int i = 0; i < nodesTag.size(); i++) {
                    CompoundTag nTag = nodesTag.getCompound(i);
                    java.util.UUID id = nTag.getUUID("Id");
                    Node localNode = graph.getNode(id);
                    if (localNode != null) {
                        localNode.value = nTag.getDouble("Value");
                    }
                }
            } else {
                // Full load/overwrite
                graph.nodes.clear();
                graph.connections.clear();
                NodeGraph loaded = new NodeGraph(graphTag, registries);
                graph.nodes.addAll(loaded.nodes);
                graph.connections.addAll(loaded.connections);
                graph.clearCache();
            }
        }
    }
}
