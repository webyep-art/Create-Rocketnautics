package dev.devce.rocketnautics.content.physics;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.commands.ShipCopyPasteCommand;
import dev.devce.rocketnautics.network.DebugLogPayload;
import dev.devce.rocketnautics.network.SeamlessTransitionPayload;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.loading.FMLPaths;
import org.joml.Vector3d;
import org.joml.Quaterniond;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles interplanetary transitions and ship persistence across dimensions.
 */
@EventBusSubscriber(modid = RocketNautics.MODID)
public class SpaceTransitionHandler {
    
    // Thresholds
    private static final double OVERWORLD_SPACE_Y = 20000.0;
    private static final double SPACE_EXIT_Y = 0.0;
    private static final double TRANSITION_SAFE_OFFSET = 50.0;
    private static final int REBUILD_DELAY_TICKS = 20;
    private static final int SEATING_RECOVERY_TIMEOUT = 40;

    // NBT Keys
    private static final String KEY_PLAYER_REL_X = "player_rel_x";
    private static final String KEY_PLAYER_REL_Y = "player_rel_y";
    private static final String KEY_PLAYER_REL_Z = "player_rel_z";
    private static final String KEY_SHIP_ENTITIES = "ship_entities";
    private static final String KEY_PLOT_REL_X = "plot_rel_x";
    private static final String KEY_PLOT_REL_Y = "plot_rel_y";
    private static final String KEY_PLOT_REL_Z = "plot_rel_z";
    private static final String KEY_WAS_SEATED = "was_seated";
    private static final String KEY_VEHICLE_TYPE = "vehicle_type";
    private static final String KEY_OLD_PLOT_X = "old_plot_x";
    private static final String KEY_OLD_PLOT_Z = "old_plot_z";
    private static final String KEY_OLD_POSE = "old_pose";
    private static final String KEY_POSE_QX = "qx";
    private static final String KEY_POSE_QY = "qy";
    private static final String KEY_POSE_QZ = "qz";
    private static final String KEY_POSE_QW = "qw";

    private static final ResourceKey<Level> SPACE_DIM = ResourceKey.create(Registries.DIMENSION, 
        ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "space"));

    private static final Path SHIPS_DIR = FMLPaths.CONFIGDIR.get().resolve("rocketnautics_ships");
    
    private static final Map<UUID, Integer> PENDING_REBUILDS = new HashMap<>();
    private static final Map<UUID, TeleportTask> PENDING_TELEPORTS = new HashMap<>();
    private static final Map<UUID, SeatingTask> PENDING_SEATING = new HashMap<>();

    private record TeleportTask(ResourceKey<Level> targetDim, double targetY) {}
    private record SeatingTask(UUID shipUUID, double relPlotX, double relPlotY, double relPlotZ, String vehicleType, int ticksLeft, boolean blockClicked) {}

    private static void sendDebug(ServerPlayer player, String msg, int color) {
        PacketDistributor.sendToPlayer(player, new DebugLogPayload(msg, color));
        RocketNautics.LOGGER.info("[SPACE TRANSITION] {}", msg);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        ServerLevel currentLevel = (ServerLevel) player.level();

        // 1. Process Teleport Tasks
        if (PENDING_TELEPORTS.containsKey(player.getUUID())) {
            executeTeleport(player, PENDING_TELEPORTS.remove(player.getUUID()));
            return;
        }

        // 2. Process Seating Tasks
        if (PENDING_SEATING.containsKey(player.getUUID())) {
            processSeating(player);
            return;
        }

        // 3. Process Rebuild Tasks
        if (PENDING_REBUILDS.containsKey(player.getUUID())) {
            processRebuildCounter(player, currentLevel);
            return;
        }

        // 4. Detect Transition Conditions
        checkTransitionConditions(player, currentLevel);
    }

    private static void checkTransitionConditions(ServerPlayer player, ServerLevel level) {
        double y = player.getY();
        if (level.dimension() == SPACE_DIM && y <= SPACE_EXIT_Y) {
            initiateTransition(player, level, Level.OVERWORLD, OVERWORLD_SPACE_Y - TRANSITION_SAFE_OFFSET);
        } else if (level.dimension() == Level.OVERWORLD && y >= OVERWORLD_SPACE_Y) {
            initiateTransition(player, level, SPACE_DIM, 10.0);
        }
    }

    private static void processRebuildCounter(ServerPlayer player, ServerLevel level) {
        int ticksLeft = PENDING_REBUILDS.get(player.getUUID());
        if (ticksLeft <= 0) {
            PENDING_REBUILDS.remove(player.getUUID());
            File transitionFile = getTransitionFile(player.getUUID());
            if (transitionFile.exists()) {
                rebuildShipAfterTransition(player, level, transitionFile);
            }
        } else {
            PENDING_REBUILDS.put(player.getUUID(), ticksLeft - 1);
            if (ticksLeft % 20 == 0) {
                sendDebug(player, "Syncing ship state: " + (ticksLeft / 20) + "s...", 0xFFFF55);
            }
        }
    }

    private static void initiateTransition(ServerPlayer player, ServerLevel fromLevel, ResourceKey<Level> toDim, double targetY) {
        sendDebug(player, "Initiating dimension jump...", 0x55FF55);
        
        ServerSubLevel ship = findShipUnderPlayer(player, fromLevel);
        if (ship != null) {
            saveShipForTransition(player, ship);
            destroyShipInSourceDimension(ship);
        }

        PacketDistributor.sendToPlayer(player, new SeamlessTransitionPayload(true));
        PENDING_TELEPORTS.put(player.getUUID(), new TeleportTask(toDim, targetY));
    }

    private static ServerSubLevel findShipUnderPlayer(ServerPlayer player, ServerLevel level) {
        ServerSubLevel ship = (ServerSubLevel) Sable.HELPER.getContaining(level, player.blockPosition());
        if (ship == null && player.getVehicle() != null) {
            ship = (ServerSubLevel) Sable.HELPER.getContaining(level, player.getVehicle().blockPosition());
        }
        return ship;
    }

    private static void destroyShipInSourceDimension(ServerSubLevel ship) {
        ship.deleteAllEntities();
        ship.getPlot().destroyAllBlocks();
        ship.markRemoved();
    }

    private static void executeTeleport(ServerPlayer player, TeleportTask task) {
        ServerLevel toLevel = player.getServer().getLevel(task.targetDim);
        if (toLevel == null) return;
        
        DimensionTransition transition = new DimensionTransition(
            toLevel, 
            new Vec3(player.getX(), task.targetY, player.getZ()), 
            player.getDeltaMovement(), 
            player.getYRot(), 
            player.getXRot(), 
            DimensionTransition.DO_NOTHING
        );
        player.changeDimension(transition);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PacketDistributor.sendToPlayer(player, new SeamlessTransitionPayload(false));
        PENDING_REBUILDS.put(player.getUUID(), REBUILD_DELAY_TICKS);
    }

    private static void saveShipForTransition(ServerPlayer player, ServerSubLevel ship) {
        try {
            CompoundTag tag = ship.getPlot().save();
            Pose3d pose = ship.logicalPose();
            
            // Save relative player position
            Vec3 playerWorldPos = Sable.HELPER.projectOutOfSubLevel(player.level(), player.position());
            tag.putDouble(KEY_PLAYER_REL_X, playerWorldPos.x - pose.position().x);
            tag.putDouble(KEY_PLAYER_REL_Y, playerWorldPos.y - pose.position().y);
            tag.putDouble(KEY_PLAYER_REL_Z, playerWorldPos.z - pose.position().z);
            
            saveShipEntities(player, ship, tag);
            saveShipMetadata(ship, pose, tag);

            NbtIo.writeCompressed(tag, getTransitionFile(player.getUUID()).toPath());
        } catch (IOException e) {
            sendDebug(player, "CRITICAL: Ship save failed!", 0xFF5555);
            RocketNautics.LOGGER.error("Failed to save ship for transition", e);
        }
    }

    private static void saveShipEntities(ServerPlayer player, ServerSubLevel ship, CompoundTag tag) {
        ListTag entityList = new ListTag();
        ChunkPos minChunk = ship.getPlot().getChunkMin();
        Entity vehicle = player.getVehicle();

        // Save vehicle player is sitting in
        if (vehicle != null) {
            CompoundTag vehicleTag = new CompoundTag();
            if (vehicle.saveAsPassenger(vehicleTag)) {
                tag.putBoolean(KEY_WAS_SEATED, true);
                tag.putString(KEY_VEHICLE_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString());
                
                double relX = vehicle.getX() - minChunk.getMinBlockX();
                double relY = vehicle.getY();
                double relZ = vehicle.getZ() - minChunk.getMinBlockZ();
                
                vehicleTag.putDouble(KEY_PLOT_REL_X, relX);
                vehicleTag.putDouble(KEY_PLOT_REL_Y, relY);
                vehicleTag.putDouble(KEY_PLOT_REL_Z, relZ);
                entityList.add(vehicleTag);
                
                // Redundant check for safety
                tag.putDouble("vehicle_plot_rel_x", relX);
                tag.putDouble("vehicle_plot_rel_y", relY);
                tag.putDouble("vehicle_plot_rel_z", relZ);
            }
        }

        // Save other entities in plot area
        AABB plotBounds = new AABB(minChunk.getMinBlockX(), -64, minChunk.getMinBlockZ(), minChunk.getMaxBlockX() + 1, 320, minChunk.getMaxBlockZ() + 1);
        for (Entity e : ((ServerLevel)player.level()).getEntitiesOfClass(Entity.class, plotBounds)) {
            if (e != player && e != vehicle) {
                CompoundTag entityTag = new CompoundTag();
                if (e.saveAsPassenger(entityTag)) {
                    entityTag.putDouble(KEY_PLOT_REL_X, e.getX() - minChunk.getMinBlockX());
                    entityTag.putDouble(KEY_PLOT_REL_Y, e.getY());
                    entityTag.putDouble(KEY_PLOT_REL_Z, e.getZ() - minChunk.getMinBlockZ());
                    entityList.add(entityTag);
                }
            }
        }
        tag.put(KEY_SHIP_ENTITIES, entityList);
    }

    private static void saveShipMetadata(ServerSubLevel ship, Pose3d pose, CompoundTag tag) {
        tag.putInt(KEY_OLD_PLOT_X, ship.getPlot().plotPos.x);
        tag.putInt(KEY_OLD_PLOT_Z, ship.getPlot().plotPos.z);
        
        CompoundTag poseTag = new CompoundTag();
        poseTag.putDouble(KEY_POSE_QX, pose.orientation().x);
        poseTag.putDouble(KEY_POSE_QY, pose.orientation().y);
        poseTag.putDouble(KEY_POSE_QZ, pose.orientation().z);
        poseTag.putDouble(KEY_POSE_QW, pose.orientation().w);
        tag.put(KEY_OLD_POSE, poseTag);
    }

    private static void rebuildShipAfterTransition(ServerPlayer player, ServerLevel level, File file) {
        try {
            CompoundTag tag = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            
            // Calculate new pose based on player position
            Pose3d newPose = calculateNewPose(player, tag);
            
            // Allocate and load ship
            ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
            ServerSubLevel newShip = (ServerSubLevel) container.allocateNewSubLevel(newPose);
            
            ChunkPos sourcePlotPos = new ChunkPos(tag.getInt(KEY_OLD_PLOT_X), tag.getInt(KEY_OLD_PLOT_Z));
            ShipCopyPasteCommand.remapBlockEntityPositions(tag, sourcePlotPos, newShip.getPlot().plotPos);
            newShip.getPlot().load(tag);
            
            rebuildEntities(level, newShip, tag);
            finalizeShipRebuild(player, newShip, tag, file);
            
        } catch (IOException e) {
            sendDebug(player, "CRITICAL: Ship rebuild failed!", 0xFF5555);
            RocketNautics.LOGGER.error("Failed to rebuild ship after transition", e);
        }
    }

    private static Pose3d calculateNewPose(ServerPlayer player, CompoundTag tag) {
        double px = player.getX() - tag.getDouble(KEY_PLAYER_REL_X);
        double py = player.getY() - tag.getDouble(KEY_PLAYER_REL_Y);
        double pz = player.getZ() - tag.getDouble(KEY_PLAYER_REL_Z);
        
        CompoundTag poseTag = tag.getCompound(KEY_OLD_POSE);
        Quaterniond rot = new Quaterniond(
            poseTag.getDouble(KEY_POSE_QX), 
            poseTag.getDouble(KEY_POSE_QY), 
            poseTag.getDouble(KEY_POSE_QZ), 
            poseTag.getDouble(KEY_POSE_QW)
        );
        
        return new Pose3d(new Vector3d(px, py, pz), rot, new Vector3d(0, 0, 0), new Vector3d(1, 1, 1));
    }

    private static void rebuildEntities(ServerLevel level, ServerSubLevel ship, CompoundTag tag) {
        ListTag entityList = tag.getList(KEY_SHIP_ENTITIES, Tag.TAG_COMPOUND);
        ChunkPos newMinChunk = ship.getPlot().getChunkMin();
        
        for (int i = 0; i < entityList.size(); i++) {
            CompoundTag entityTag = entityList.getCompound(i);
            EntityType.create(entityTag, level).ifPresent(e -> {
                double ex = newMinChunk.getMinBlockX() + entityTag.getDouble(KEY_PLOT_REL_X);
                double ey = entityTag.getDouble(KEY_PLOT_REL_Y);
                double ez = newMinChunk.getMinBlockZ() + entityTag.getDouble(KEY_PLOT_REL_Z);
                e.setPos(ex, ey, ez);
                ((EntityMovementExtension) e).sable$setTrackingSubLevel(ship);
                level.addFreshEntity(e);
            });
        }
    }

    private static void finalizeShipRebuild(ServerPlayer player, ServerSubLevel ship, CompoundTag tag, File file) {
        ship.getPlot().updateBoundingBox();
        ship.updateBoundingBox();
        
        if (tag.getBoolean(KEY_WAS_SEATED)) {
            PENDING_SEATING.put(player.getUUID(), new SeatingTask(
                ship.getUniqueId(), 
                tag.getDouble("vehicle_plot_rel_x"), 
                tag.getDouble("vehicle_plot_rel_y"), 
                tag.getDouble("vehicle_plot_rel_z"), 
                tag.getString(KEY_VEHICLE_TYPE), 
                SEATING_RECOVERY_TIMEOUT, 
                false
            ));
            sendDebug(player, "Seating recovery scheduled...", 0xFFFF55);
        }
        
        file.delete();
        sendDebug(player, "TRANSITION COMPLETE!", 0xFFAA00);
    }

    private static void processSeating(ServerPlayer player) {
        SeatingTask task = PENDING_SEATING.get(player.getUUID());
        if (task == null) return;

        if (task.ticksLeft > 0) {
            if (searchAndSeat(player, task)) {
                PENDING_SEATING.remove(player.getUUID());
                return;
            }
            // Update task with decremented ticks
            PENDING_SEATING.put(player.getUUID(), new SeatingTask(task.shipUUID, task.relPlotX, task.relPlotY, task.relPlotZ, task.vehicleType, task.ticksLeft - 1, task.blockClicked));
            return;
        }
        
        // Final fallback: Try block seating or magnetic boots
        handleSeatingFallback(player, task);
        PENDING_SEATING.remove(player.getUUID());
    }

    private static void handleSeatingFallback(ServerPlayer player, SeatingTask task) {
        if (player.getVehicle() != null) return;

        if (!task.blockClicked && tryBlockSeating(player, task)) {
            // Give it one more try after block interaction
            PENDING_SEATING.put(player.getUUID(), new SeatingTask(task.shipUUID, task.relPlotX, task.relPlotY, task.relPlotZ, task.vehicleType, 20, true));
            return;
        }
        
        applyMagneticBoots(player, task);
    }

    private static void applyMagneticBoots(ServerPlayer player, SeatingTask task) {
        ServerLevel level = (ServerLevel) player.level();
        ServerSubLevel ship = findShipByUUID(level, task.shipUUID);
        if (ship == null) return;

        ChunkPos minChunk = ship.getPlot().getChunkMin();
        double tx = minChunk.getMinBlockX() + task.relPlotX;
        double ty = task.relPlotY;
        double tz = minChunk.getMinBlockZ() + task.relPlotZ;

        Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(level, new Vec3(tx, ty, tz));
        player.teleportTo(worldPos.x, worldPos.y, worldPos.z);
        ((EntityMovementExtension) player).sable$setTrackingSubLevel(ship);
        player.setPose(net.minecraft.world.entity.Pose.SITTING);
        
        sendDebug(player, "Magnetic Boots Activated! (Emergency Sitting)", 0xFFAA00);
    }

    private static boolean searchAndSeat(ServerPlayer player, SeatingTask task) {
        ServerLevel level = (ServerLevel) player.level();
        ServerSubLevel ship = findShipByUUID(level, task.shipUUID);
        if (ship == null) return false;
        
        ChunkPos minChunk = ship.getPlot().getChunkMin();
        double tx = minChunk.getMinBlockX() + task.relPlotX;
        double ty = task.relPlotY;
        double tz = minChunk.getMinBlockZ() + task.relPlotZ;

        for (Entity e : level.getEntities().getAll()) {
            if (e != player && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(task.vehicleType)) {
                SubLevel trackingShip = ((EntityMovementExtension) e).sable$getTrackingSubLevel();
                if (trackingShip != null && trackingShip.getUniqueId().equals(task.shipUUID)) {
                    if (e.distanceToSqr(tx, ty, tz) < 25.0) {
                        player.startRiding(e, true);
                        sendDebug(player, "Seating recovery successful!", 0x55FF55);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean tryBlockSeating(ServerPlayer player, SeatingTask task) {
        ServerLevel level = (ServerLevel) player.level();
        ServerSubLevel ship = findShipByUUID(level, task.shipUUID);
        if (ship == null) return false;

        ChunkPos minChunk = ship.getPlot().getChunkMin();
        double tx = minChunk.getMinBlockX() + task.relPlotX;
        double ty = task.relPlotY;
        double tz = minChunk.getMinBlockZ() + task.relPlotZ;
        BlockPos targetPos = BlockPos.containing(tx, ty, tz);
        BlockState state = level.getBlockState(targetPos);
        
        sendDebug(player, "Attempting block interaction: " + state.getBlock().getName().getString(), 0xFFFF55);
        
        // Attempt Create Seat interaction via reflection
        try {
            Class<?> seatBlockClass = Class.forName("com.simibubi.create.content.contraptions.components.seats.SeatBlock");
            if (seatBlockClass.isInstance(state.getBlock())) {
                Method sitDown = seatBlockClass.getMethod("sitDown", Level.class, BlockPos.class, Entity.class);
                sitDown.invoke(null, level, targetPos, player);
                if (player.getVehicle() != null) return true;
            }
        } catch (Exception ignored) {}

        // General block use fallback
        Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(level, new Vec3(tx, ty, tz));
        BlockHitResult hit = new BlockHitResult(worldPos, Direction.UP, targetPos, false);
        state.useWithoutItem(level, player, hit);
        
        return true;
    }

    private static ServerSubLevel findShipByUUID(ServerLevel level, UUID uuid) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null) {
            for (SubLevel sl : container.getAllSubLevels()) {
                if (sl.getUniqueId().equals(uuid)) return (ServerSubLevel) sl;
            }
        }
        return null;
    }

    private static File getTransitionFile(UUID playerUUID) {
        return SHIPS_DIR.resolve("transition_" + playerUUID.toString() + ".nbt").toFile();
    }
}
