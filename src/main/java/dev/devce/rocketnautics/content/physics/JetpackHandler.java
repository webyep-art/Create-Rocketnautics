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

@EventBusSubscriber(modid = RocketNautics.MODID)
public class JetpackHandler {
    private static final Set<UUID> SERVER_ACTIVE_JETPACKS = new HashSet<>();
    private static final Map<Integer, Boolean> CLIENT_ACTIVE_JETPACKS = new HashMap<>();

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

    private static void applyJetpackPhysics(Player player) {
        
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof dev.devce.rocketnautics.content.items.JetpackItem)) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        boolean thrusting = ((dev.devce.rocketnautics.mixin.LivingEntityAccessor) player).rocketnautics$isJumping();
        boolean sprinting = player.isSprinting();

        Vec3 thrust = Vec3.ZERO;
        if (thrusting) {
            Vec3 look = player.getLookAngle();
            
            
            double thrustPower = sprinting ? dev.devce.rocketnautics.RocketConfig.SERVER.jetpackSprintThrust.get() : dev.devce.rocketnautics.RocketConfig.SERVER.jetpackThrust.get();
            thrust = look.scale(thrustPower);
            
            
            if (look.y > -0.5) {
                thrust = thrust.add(0, 0.08, 0);
            }
        }

        
        double drag = sprinting ? 0.98 : 0.95;
        
        
        Vec3 newMotion = motion.scale(drag).add(thrust);
        
        
        double maxSpeed = sprinting ? 3.0 : 1.2;
        if (newMotion.length() > maxSpeed) {
            newMotion = newMotion.normalize().scale(maxSpeed);
        }

        player.setDeltaMovement(newMotion);
        player.fallDistance = 0;
    }

    private static void spawnJetpackParticles(Player player) {
        boolean thrusting = ((dev.devce.rocketnautics.mixin.LivingEntityAccessor) player).rocketnautics$isJumping();
        if (!thrusting) return;

        
        float yaw = player.yBodyRot;
        float rad = yaw * (float) (Math.PI / 180.0);
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);

        
        double backDist = 0.35;
        double sideDist = 0.22;
        double height = 0.7;

        
        double lx = player.getX() + (cos * sideDist + sin * backDist);
        double lz = player.getZ() + (sin * sideDist - cos * backDist);
        
        double rx = player.getX() + (-cos * sideDist + sin * backDist);
        double rz = player.getZ() + (-sin * sideDist - cos * backDist);

        
        player.level().addParticle(ParticleTypes.CLOUD, lx, player.getY() + height, lz, 0, -0.15, 0);
        player.level().addParticle(ParticleTypes.CLOUD, rx, player.getY() + height, rz, 0, -0.15, 0);
    }
}
