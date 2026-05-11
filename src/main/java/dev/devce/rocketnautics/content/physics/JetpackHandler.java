package dev.devce.rocketnautics.content.physics;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.network.JetpackPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.particles.ParticleTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * System for managing jetpack flight mechanics.
 * Handles state synchronization between server and client, flight physics (thrust/drag),
 * and exhaust particle effects.
 */
@EventBusSubscriber(modid = RocketNautics.MODID)
public class JetpackHandler {
    private static final Set<UUID> SERVER_ACTIVE_JETPACKS = new HashSet<>();
    private static final Map<Integer, Boolean> CLIENT_ACTIVE_JETPACKS = new HashMap<>();

    /**
     * Toggles the jetpack state for a player and syncs the change to all clients.
     */
    public static void toggle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        boolean newState = !SERVER_ACTIVE_JETPACKS.contains(uuid);
        if (newState) {
            SERVER_ACTIVE_JETPACKS.add(uuid);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("rocketnautics.jetpack.enabled").withStyle(net.minecraft.ChatFormatting.GREEN), true);
        } else {
            SERVER_ACTIVE_JETPACKS.remove(uuid);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("rocketnautics.jetpack.disabled").withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        syncToAll(player, newState);
    }

    public static boolean isActive(Player player) {
        if (player == null) return false;
        
        
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        boolean isWearing = chest.getItem() instanceof dev.devce.rocketnautics.content.items.JetpackItem;
        
        if (!isWearing) return false;

        if (player.level().isClientSide) {
            return CLIENT_ACTIVE_JETPACKS.getOrDefault(player.getId(), false);
        }
        return SERVER_ACTIVE_JETPACKS.contains(player.getUUID());
    }

    public static void setEntityActive(int entityId, boolean active) {
        if (active) {
            CLIENT_ACTIVE_JETPACKS.put(entityId, true);
        } else {
            CLIENT_ACTIVE_JETPACKS.remove(entityId);
        }
    }

    private static void syncToAll(ServerPlayer player, boolean active) {
        PacketDistributor.sendToAllPlayers(new JetpackPayload(player.getId(), active));
    }

    @SubscribeEvent
    public static void onPlayerTrack(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && isActive(target)) {
            PacketDistributor.sendToPlayer((ServerPlayer) event.getEntity(), new JetpackPayload(target.getId(), true));
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;

        
        if (isActive(player)) {
            applyJetpackPhysics(player);
            if (player.level().isClientSide && player.level().getGameTime() % 2 == 0) {
                spawnJetpackParticles(player);
            }
        }
    }

    /**
     * Calculates and applies jetpack flight physics to the player.
     * Uses the player's look vector for thrust direction and applies drag to simulate flight.
     */
    private static void applyJetpackPhysics(Player player) {
        
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof dev.devce.rocketnautics.content.items.JetpackItem)) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        // Check if player is holding the jump key using a Mixin accessor
        boolean thrusting = ((dev.devce.rocketnautics.mixin.LivingEntityAccessor) player).rocketnautics$isJumping();
        boolean sprinting = player.isSprinting();

        Vec3 thrust = Vec3.ZERO;
        if (thrusting) {
            Vec3 look = player.getLookAngle();
            
            // Scaled thrust power from config
            double thrustPower = sprinting ? dev.devce.rocketnautics.RocketConfig.SERVER.jetpackSprintThrust.get() : dev.devce.rocketnautics.RocketConfig.SERVER.jetpackThrust.get();
            thrust = look.scale(thrustPower);
            
            // Add a small constant upward lift to assist horizontal flight
            if (look.y > -0.5) {
                thrust = thrust.add(0, 0.08, 0);
            }
        }

        // Apply drag: sprinting has less air resistance
        double drag = sprinting ? 0.98 : 0.95;
        
        // Disable drag in space or high altitude
        if (player.getY() > 2000 || player.level().dimension().location().getPath().equals("space")) {
            drag = 1.0;
        }
        
        // Final motion calculation: Motion = (OldMotion * Drag) + Thrust
        Vec3 newMotion = motion.scale(drag).add(thrust);
        
        // Speed capping for stability
        double maxSpeed = sprinting ? 3.0 : 1.2;
        if (newMotion.length() > maxSpeed) {
            newMotion = newMotion.normalize().scale(maxSpeed);
        }

        player.setDeltaMovement(newMotion);
        player.fallDistance = 0; // Prevent fall damage while using jetpack
    }

    /**
     * Spawns exhaust cloud particles behind the player's shoulders.
     */
    private static void spawnJetpackParticles(Player player) {
        boolean thrusting = ((dev.devce.rocketnautics.mixin.LivingEntityAccessor) player).rocketnautics$isJumping();
        if (!thrusting) return;

        // Calculate offset positions relative to player body rotation
        float yaw = player.yBodyRot;
        float rad = yaw * (float) (Math.PI / 180.0);
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);

        // Position offsets for the two nozzles
        double backDist = 0.35;
        double sideDist = 0.22;
        double height = 0.7;

        // Transform local nozzle coordinates to world coordinates
        double lx = player.getX() + (cos * sideDist + sin * backDist);
        double lz = player.getZ() + (sin * sideDist - cos * backDist);
        
        double rx = player.getX() + (-cos * sideDist + sin * backDist);
        double rz = player.getZ() + (-sin * sideDist - cos * backDist);

        // Spawn particles
        player.level().addParticle(ParticleTypes.CLOUD, lx, player.getY() + height, lz, 0, -0.15, 0);
        player.level().addParticle(ParticleTypes.CLOUD, rx, player.getY() + height, rz, 0, -0.15, 0);
    }
}
