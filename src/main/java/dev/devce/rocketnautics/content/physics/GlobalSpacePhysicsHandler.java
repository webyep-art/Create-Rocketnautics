package dev.devce.rocketnautics.content.physics;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.network.ReentryHeatPayload;
import dev.devce.rocketnautics.registry.RocketParticles;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Handles global space physics including zero-G simulation and atmospheric re-entry heat.
 */
@EventBusSubscriber(modid = RocketNautics.MODID)
public class GlobalSpacePhysicsHandler {

    // Thresholds and constants
    private static final double SPACE_GRAVITY_START_Y = 2000.0;
    private static final double SPACE_GRAVITY_FULL_Y = 5000.0;
    private static final double REENTRY_HEAT_START_Y = 1000.0;
    private static final double REENTRY_HEAT_END_Y = 2500.0;
    private static final double REENTRY_SPEED_THRESHOLD = 60.0;
    private static final double REENTRY_FRICTION_COEFF = 0.3;
    private static final double DEFAULT_CALIBRATION = 0.99895;

    private static final ResourceLocation SPACE_GRAVITY_ID = ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "space_gravity");
    private static final AttributeModifier SPACE_GRAVITY_MODIFIER = new AttributeModifier(
            SPACE_GRAVITY_ID,
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private static Float gravityOverride = null;
    private static double calibrationMultiplier = DEFAULT_CALIBRATION;

    public static void setGravityOverride(float value) {
        gravityOverride = value;
    }

    public static void resetGravityOverride() {
        gravityOverride = null;
    }

    public static void setCalibration(double value) {
        calibrationMultiplier = value;
    }

    public static void init() {
        SableEventPlatform.INSTANCE.onPhysicsTick((physicsSystem, timeStep) -> {
            ServerLevel level = physicsSystem.getLevel();
            SubLevelContainer container = SubLevelContainer.getContainer(level);

            if (container == null) return;

            for (SubLevel subLevel : container.getAllSubLevels()) {
                if (subLevel instanceof ServerSubLevel serverSubLevel) {
                    RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);
                    if (handle != null) {
                        processSubLevelPhysics(serverSubLevel, handle, level, timeStep);
                    }
                }
            }
        });
    }

    private static void processSubLevelPhysics(ServerSubLevel subLevel, RigidBodyHandle handle, ServerLevel level, double timeStep) {
        Vector3d worldPos = subLevel.logicalPose().position();
        if (worldPos == null) return;

        applyZeroGravity(subLevel, handle, level, worldPos, timeStep);
        applyReentryHeat(subLevel, handle, level, worldPos, timeStep);
    }

    private static void applyZeroGravity(ServerSubLevel subLevel, RigidBodyHandle handle, ServerLevel level, Vector3d worldPos, double timeStep) {
        double gravityFactor = calculateGravityFactor(level, worldPos.y());
        if (gravityFactor <= 0.0) return;

        double mass = subLevel.getMassTracker().getMass();
        Vector3d gravityVector = DimensionPhysicsData.getGravity(level);

        // Calculate anti-gravity force to counteract world gravity
        Vector3d antiGravityImpulse = new Vector3d(gravityVector)
                .mul(-1.0 * mass * gravityFactor * timeStep * calibrationMultiplier);

        // Transform to local space and apply
        Quaterniond orientation = subLevel.logicalPose().orientation();
        Vector3d localImpulse = orientation.transformInverse(antiGravityImpulse, new Vector3d());
        handle.applyLinearImpulse(localImpulse);
    }

    private static double calculateGravityFactor(ServerLevel level, double y) {
        if (gravityOverride != null) {
            return 1.0 - gravityOverride;
        }

        // Always zero-G in space dimension
        if (level.dimension().location().getPath().equals("space")) {
            return 1.0;
        }

        // Gradual transition in Overworld
        if (y <= SPACE_GRAVITY_START_Y) return 0.0;
        return Math.clamp((y - SPACE_GRAVITY_START_Y) / (SPACE_GRAVITY_FULL_Y - SPACE_GRAVITY_START_Y), 0.0, 1.0);
    }

    private static void applyReentryHeat(ServerSubLevel subLevel, RigidBodyHandle handle, ServerLevel level, Vector3d worldPos, double timeStep) {
        double y = worldPos.y();
        if (y > REENTRY_HEAT_END_Y || y < REENTRY_HEAT_START_Y) return;

        Vector3d velocity = new Vector3d(handle.getLinearVelocity());
        double descentSpeed = -velocity.y();

        if (descentSpeed > REENTRY_SPEED_THRESHOLD) {
            float intensity = (float) Math.clamp((descentSpeed - REENTRY_SPEED_THRESHOLD) / REENTRY_SPEED_THRESHOLD, 0.0, 1.0);
            
            // Apply air resistance friction
            Vector3d friction = new Vector3d(0, descentSpeed * intensity * REENTRY_FRICTION_COEFF, 0);
            handle.applyLinearImpulse(friction.mul(subLevel.getMassTracker().getMass() * timeStep));

            // Apply heat damage
            if (intensity > 0.3 && level.getGameTime() % 20 == 0) {
                AABB damageArea = new AABB(worldPos.x - 4, worldPos.y - 4, worldPos.z - 4, worldPos.x + 4, worldPos.y + 4, worldPos.z + 4);
                level.getEntitiesOfClass(LivingEntity.class, damageArea).forEach(entity -> {
                    entity.hurt(level.damageSources().onFire(), intensity * 3.0f);
                });
            }
            
            // Sync effects to nearby players
            for (ServerPlayer player : level.players()) {
                PacketDistributor.sendToPlayer(player, new ReentryHeatPayload(worldPos.x, worldPos.y, worldPos.z, intensity));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (shouldApplyZeroG(entity)) {
            applyEntityZeroG(entity);
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living) {
            updateLivingEntityGravityModifier(living);
            applyFallingHeatDamage(living);
        }
    }

    private static boolean shouldApplyZeroG(Entity entity) {
        if (gravityOverride != null) {
            return gravityOverride < 0.1f;
        }
        return entity.getY() > 1000 || entity.level().dimension().location().getPath().equals("space");
    }

    private static void applyEntityZeroG(Entity entity) {
        if (entity instanceof ItemEntity item) {
            // Counteract gravity for items (0.04 is standard gravity per tick)
            item.setDeltaMovement(item.getDeltaMovement().add(0, 0.039, 0));
        }
    }

    private static void updateLivingEntityGravityModifier(LivingEntity living) {
        boolean shouldFloat = shouldApplyZeroG(living);
        var gravityAttr = living.getAttribute(Attributes.GRAVITY);
        
        if (gravityAttr != null) {
            if (shouldFloat && !gravityAttr.hasModifier(SPACE_GRAVITY_ID)) {
                gravityAttr.addTransientModifier(SPACE_GRAVITY_MODIFIER);
            } else if (!shouldFloat && gravityAttr.hasModifier(SPACE_GRAVITY_ID)) {
                gravityAttr.removeModifier(SPACE_GRAVITY_ID);
            }
        }
    }

    private static void applyFallingHeatDamage(LivingEntity entity) {
        double y = entity.getY();
        if (y > REENTRY_HEAT_START_Y && y < REENTRY_HEAT_END_Y && entity.getDeltaMovement().y() < -3.0) {
            double speed = -entity.getDeltaMovement().y();
            float intensity = (float) Math.clamp((speed - 3.0) / 1.0, 0.0, 1.0);
            
            if (intensity > 0.1) {
                entity.setRemainingFireTicks(40);
                if (entity.level().getGameTime() % 10 == 0) {
                    entity.hurt(entity.level().damageSources().onFire(), intensity * 2.0f);
                }
                if (entity.level().isClientSide) {
                    spawnHeatParticles(entity);
                }
            }
        }
    }

    private static void spawnHeatParticles(Entity entity) {
        for (int i = 0; i < 5; i++) {
            entity.level().addParticle(RocketParticles.BLUE_FLAME.get(), 
                entity.getX(), entity.getY(), entity.getZ(), 0, 0.1, 0);
        }
    }
}
