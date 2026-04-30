package dev.devce.rocketnautics.event;

import dev.devce.rocketnautics.registry.RocketItems;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RopeHandler {

    private static final Map<UUID, RopeState> ROPE_STATES = new HashMap<>();
    private static final ResourceLocation SIMULATED_ROPE_CONNECTOR_ID =
            ResourceLocation.fromNamespaceAndPath("simulated", "rope_connector");

    private static final double DEFAULT_ROPE_LENGTH = 8.0;
    private static final double MIN_ROPE_LENGTH = 1.25;
    private static final double BASE_PULL_STRENGTH = 0.16;
    private static final double STRETCH_PULL_STRENGTH = 0.18;
    private static final double MAX_PULL_STRENGTH = 0.90;
    private static final double MAX_RELATIVE_SPEED = 7.20;
    private static final double DAMPING = 0.992;
    private static final double TAUT_THRESHOLD = 0.98;
    private static final double CLIMB_ACCELERATION = 0.28;
    private static final double REEL_SPEED = 0.45;
    private static final double INPUT_DEADZONE = 0.15;

    private RopeHandler() {
    }

    private static final class RopeState {
        private final BlockPos anchorPos;
        private Vector3d anchor;
        private double ropeLength;
        private final double maxRopeLength;

        private RopeState(BlockPos anchorPos, Vector3d anchor, double ropeLength, double maxRopeLength) {
            this.anchorPos = anchorPos;
            this.anchor = anchor;
            this.ropeLength = ropeLength;
            this.maxRopeLength = maxRopeLength;
        }
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RopeHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RopeHandler::onRightClickBlock);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        UUID id = player.getUUID();
        RopeState state = ROPE_STATES.get(id);

        boolean wearing = player.getItemBySlot(EquipmentSlot.CHEST).is(RocketItems.SPACE_CHESTPLATE.get());

        if (!wearing) {
            ROPE_STATES.remove(id);
            return;
        }

        if (state == null || state.anchor == null) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (!isRopeConnector(player.level(), state.anchorPos)) {
            detachIfPresent(player);
            return;
        }


        double climbInput = getClimbInput(player);
        if (climbInput != 0.0 && isInZeroG(player)) {
            state.ropeLength = clamp(state.ropeLength - climbInput * REEL_SPEED, MIN_ROPE_LENGTH, state.maxRopeLength);
        }

        applyGrapple(player, state, climbInput);

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 projectedAnchor = projectAnchor(player.level(), state.anchor);
            spawnRopeParticles(serverLevel, player.position().add(0, 1.5, 0), projectedAnchor);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(RocketItems.SPACE_CHESTPLATE.get())) {
            return;
        }

        if (!player.getItemBySlot(EquipmentSlot.MAINHAND).is(RocketItems.TETHER.get())) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!isRopeConnector(level, pos)) {
            showStatus(player, "message.rocketnautics.tether_invalid_anchor");
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        Vec3 anchor = blockCenter(pos);

        UUID id = player.getUUID();
        double ropeLengthSqr = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, player.position(), anchor);
        double ropeLength = Math.sqrt(Math.max(0.0, ropeLengthSqr)) * 1.5;
        if (!Double.isFinite(ropeLength)) {
            ropeLength = DEFAULT_ROPE_LENGTH;
        }

        double clampedLength = Math.max(MIN_ROPE_LENGTH, ropeLength);
        ROPE_STATES.put(id, new RopeState(
                pos.immutable(),
                new Vector3d(anchor.x, anchor.y, anchor.z),
                clampedLength,
                clampedLength
        ));

        showStatus(player, "message.rocketnautics.tether_attached");
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static Vec3 getProjectedAnchor(Player player) {
        RopeState state = ROPE_STATES.get(player.getUUID());
        if (state == null || state.anchor == null) {
            return null;
        }
        return projectAnchor(player.level(), state.anchor);
    }

    private static void applyGrapple(Player player, RopeState state, double climbInput) {
        Level level = player.level();
        Vector3d anchor = state.anchor;
        double ropeLength = state.ropeLength;
        Vec3 anchorRaw = new Vec3(anchor.x, anchor.y, anchor.z);
        Vec3 playerPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, player.position());
        Vec3 target = SableCompanion.INSTANCE.projectOutOfSubLevel(level, anchorRaw);
        Vec3 toAnchor = target.subtract(playerPos);
        double distance = toAnchor.length();

        if (distance < 0.0001) {
            return;
        }

        Vec3 dir = toAnchor.normalize();
        Vec3 anchorVelocity = SableCompanion.INSTANCE.getVelocity(level, anchorRaw);
        Vec3 relativeVel = player.getDeltaMovement().subtract(anchorVelocity);
        double radialSpeed = relativeVel.dot(dir);

        Vec3 newRelativeVel = relativeVel;

        if (climbInput != 0.0 && isInZeroG(player)) {
            newRelativeVel = newRelativeVel.add(dir.scale(climbInput * CLIMB_ACCELERATION));
        }

        if (distance >= ropeLength * TAUT_THRESHOLD) {
            // radialSpeed < 0 means moving away from anchor.
            if (radialSpeed < 0.0) {
                newRelativeVel = newRelativeVel.subtract(dir.scale(radialSpeed));
            }

            if (distance > ropeLength) {
                double stretch = distance - ropeLength;
                double pullStrength = Math.min(MAX_PULL_STRENGTH, BASE_PULL_STRENGTH + stretch * STRETCH_PULL_STRENGTH);
                newRelativeVel = newRelativeVel.add(dir.scale(pullStrength));
            }
        }

        newRelativeVel = newRelativeVel.scale(DAMPING);

        double relativeSpeed = newRelativeVel.length();
        if (relativeSpeed > MAX_RELATIVE_SPEED) {
            newRelativeVel = newRelativeVel.scale(MAX_RELATIVE_SPEED / relativeSpeed);
        }

        Vec3 newVel = newRelativeVel.add(anchorVelocity);

        player.setDeltaMovement(newVel);
        player.hurtMarked = true;
    }

    private static double getClimbInput(Player player) {
        double forward = player.zza;
        if (Math.abs(forward) < INPUT_DEADZONE) {
            return 0.0;
        }
        return clamp(forward, -1.0, 1.0);
    }

    private static boolean isInZeroG(Player player) {
        return !player.onGround() || player.getY() > 1000.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isRopeConnector(Level level, BlockPos pos) {
        Block ropeConnector = BuiltInRegistries.BLOCK.get(SIMULATED_ROPE_CONNECTOR_ID);
        Block block = level.getBlockState(pos).getBlock();
        if (ropeConnector != null && block == ropeConnector) {
            return true;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId != null && "rope_connector".equals(blockId.getPath())) {
            return true;
        }

        String descriptionId = block.getDescriptionId();
        if ("block.simulated.rope_connector".equals(descriptionId)) {
            return true;
        }

        String className = block.getClass().getName();
        return className.endsWith(".RopeConnectorBlock")
                || className.contains(".rope.rope_connector.");
    }

    private static void detach(Player player) {
        ROPE_STATES.remove(player.getUUID());
    }

    public static void detachIfPresent(Player player) {
        if (!ROPE_STATES.containsKey(player.getUUID())) {
            return;
        }

        detach(player);
        showStatus(player, "message.rocketnautics.tether_detached");
    }


    private static void showStatus(Player player, String translationKey) {
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private static Vec3 projectAnchor(Level level, Vector3d anchor) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vec3(anchor.x, anchor.y, anchor.z));
    }

    private static Vec3 blockCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static void spawnRopeParticles(ServerLevel level, Vec3 from, Vec3 to) {
        int segments = 12;
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }
}
