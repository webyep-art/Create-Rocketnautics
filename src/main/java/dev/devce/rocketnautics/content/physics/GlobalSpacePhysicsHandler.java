package dev.devce.rocketnautics.content.physics;

import dev.devce.rocketnautics.RocketNautics;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3d;
import org.joml.Quaterniond;

@EventBusSubscriber(modid = RocketNautics.MODID)
public class GlobalSpacePhysicsHandler {

    private static Float gravityOverride = null;
    private static double calibrationMultiplier = 0.99895;

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
            ServerLevel serverLevel = physicsSystem.getLevel();
            SubLevelContainer container = SubLevelContainer.getContainer(serverLevel);

            if (container == null)
                return;

            for (SubLevel subLevel : container.getAllSubLevels()) {
                if (subLevel instanceof ServerSubLevel serverSubLevel) {
                    RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);
                    if (handle != null) {
                        applyGlobalZeroG(serverSubLevel, handle, serverLevel, timeStep);
                    }
                }
            }
        });
    }

    private static void applyGlobalZeroG(ServerSubLevel subLevel, RigidBodyHandle handle, ServerLevel level,
            double timeStep) {
        Vector3d worldPos = subLevel.logicalPose().position();
        if (worldPos == null)
            return;

        double y = worldPos.y();
        double factor;

        if (gravityOverride != null) {
            factor = 1.0 - gravityOverride;
        } else {
            if (y <= 1000.0)
                return;
            factor = Math.clamp((y - 1000.0) / 4000.0, 0.0, 1.0);
        }

        if (factor <= 0.0)
            return;

        double mass = subLevel.getMassTracker().getMass();
        Vector3d gravityVector = DimensionPhysicsData.getGravity(level);

        Vector3d globalAntiGravityImpulse = new Vector3d(gravityVector)
                .mul(-1.0 * mass * factor * timeStep * calibrationMultiplier);

        Quaterniond orientation = subLevel.logicalPose().orientation();
        Vector3d localImpulse = orientation.transformInverse(globalAntiGravityImpulse, new Vector3d());

        handle.applyLinearImpulse(localImpulse);
    }

    private static final ResourceLocation SPACE_GRAVITY_ID = ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID,
            "space_gravity");

    private static final AttributeModifier SPACE_GRAVITY_MODIFIER = new AttributeModifier(
            SPACE_GRAVITY_ID,
            -0.98,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        boolean shouldFloat;
        if (gravityOverride != null) {
            shouldFloat = gravityOverride < 0.1f;
        } else {
            shouldFloat = entity.getY() > 1000;
        }

        if (!shouldFloat)
            return;

        if (entity instanceof LivingEntity living) {
            var gravityAttr = living.getAttribute(Attributes.GRAVITY);
            if (gravityAttr != null) {
                if (!gravityAttr.hasModifier(SPACE_GRAVITY_ID)) {
                    gravityAttr.addTransientModifier(SPACE_GRAVITY_MODIFIER);
                }
            }
        } else if (entity instanceof ItemEntity item) {
            item.setDeltaMovement(item.getDeltaMovement().add(0, 0.039, 0));
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveSpace(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living) {
            boolean shouldFloat;

            if (gravityOverride != null) {
                shouldFloat = gravityOverride < 0.1f;
            } else {
                shouldFloat = entity.getY() > 1000;
            }

            if (!shouldFloat) {
                var gravityAttr = living.getAttribute(Attributes.GRAVITY);
                if (gravityAttr != null && gravityAttr.hasModifier(SPACE_GRAVITY_ID)) {
                    gravityAttr.removeModifier(SPACE_GRAVITY_ID);
                }
            }
        }
    }
}
