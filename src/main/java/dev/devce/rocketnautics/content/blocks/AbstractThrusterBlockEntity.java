package dev.devce.rocketnautics.content.blocks;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.devce.rocketnautics.registry.RocketParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3d;

import java.util.List;

/**
 * Base class for all thrusters in RocketNautics.
 * Handles common logic for thrust, particles, sound, damage, and physics.
 */
public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity 
        implements BlockEntitySubLevelActor, IThruster, com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {

    // Constants for thrust and effects
    protected static final float THRUST_FORCE_MULTIPLIER = 10.0f;
    protected static final float VISUAL_POWER_MULTIPLIER = 2.85f;
    protected static final float DAMAGE_PER_POWER = 0.1f;
    protected static final float PUSH_STRENGTH_MULTIPLIER = 1.0f / 150.0f;
    protected static final int MAX_IGNITION_TICKS = 100;

    protected boolean currentlyBurning = false;
    protected int ignitionTicks = 0;
    private Object soundInstance;

    protected AbstractThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);

    /**
     * @return The current thrust power in Newtons (after scaling).
     */
    public abstract int getCurrentPower();

    /**
     * @return The direction in which the thrust is directed.
     */
    public Direction getThrustDirection() {
        BlockState state = getBlockState();
        if (state.hasProperty(RocketThrusterBlock.FACING)) {
            return state.getValue(RocketThrusterBlock.FACING);
        }
        return Direction.UP;
    }

    public int getWarmupTime() {
        return 40;
    }

    @Override
    public boolean isActive() {
        return currentlyBurning;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractThrusterBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.updateActiveState();
            
            // Sync LIT blockstate with active status
            boolean active = blockEntity.isActive();
            if (state.hasProperty(RocketThrusterBlock.LIT)) {
                if (state.getValue(RocketThrusterBlock.LIT) != active) {
                    level.setBlock(pos, state.setValue(RocketThrusterBlock.LIT, active), 3);
                }
            }
        }

        blockEntity.tick();
        boolean active = blockEntity.isActive();

        if (level.isClientSide()) {
            blockEntity.updateClientVisuals(active);
        }

        blockEntity.updateIgnition(active);
        
        if (active) {
            blockEntity.applyThrusterEffects(level, pos);
        }
    }

    protected void updateIgnition(boolean active) {
        if (active) {
            if (ignitionTicks < MAX_IGNITION_TICKS) ignitionTicks++;
        } else {
            if (ignitionTicks > 0) ignitionTicks--;
        }
    }

    protected abstract void updateActiveState();

    @OnlyIn(Dist.CLIENT)
    protected void updateClientVisuals(boolean active) {
        updateSound(active);
        if (active) {
            spawnThrustParticles();
        }
    }

    protected void spawnThrustParticles() {
        Level level = getLevel();
        if (level == null) return;

        BlockPos pos = getBlockPos();
        Vector3d pDir = getParticleDirection();
        RandomSource random = level.getRandom();
        int power = getCurrentPower();
        int visualPower = (int) (power * VISUAL_POWER_MULTIPLIER);

        if (random.nextFloat() < (visualPower / 100.0f)) {
            spawnSmokeParticles(pos, pDir, visualPower, random);
        }

        // Apply ground impact dust for all non-RCS engines
        float impactPower = Math.min(1.0f, getCurrentPower() / 50.0f);
        if (impactPower > 0.1f) {
            spawnGroundImpactEffects(level, pos, pDir, impactPower, random);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnGroundImpactEffects(Level level, BlockPos pos, org.joml.Vector3d pDir, float power, RandomSource random) {
        double maxDist = 20.0 * power;
        
        // On client, Minecraft.getInstance().level is the main world level
        Level worldLevel = net.minecraft.client.Minecraft.getInstance().level;
        if (worldLevel == null) return;

        // Convert local start/end to GLOBAL world coordinates
        Vec3 localStart = Vec3.atCenterOf(pos).add(pDir.x() * 0.6, pDir.y() * 0.6, pDir.z() * 0.6);
        Vec3 localEnd = localStart.add(pDir.x() * maxDist, pDir.y() * maxDist, pDir.z() * maxDist);
        
        Vec3 worldStart = Sable.HELPER.projectOutOfSubLevel(level, localStart);
        Vec3 worldEnd = Sable.HELPER.projectOutOfSubLevel(level, localEnd);

        // Raycast in the world level (this will ignore ship blocks)
        var hit = worldLevel.clip(new net.minecraft.world.level.ClipContext(
                worldStart, worldEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER, 
                net.minecraft.world.level.ClipContext.Fluid.NONE, CollisionContext.empty()));

        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            Vec3 hitPos = hit.getLocation();
            double dist = hitPos.distanceTo(worldStart);
            
            float intensity = (float) Math.max(0, 1.0 - (dist / maxDist)) * power;
            if (intensity < 0.1f) return;

            // RADIAL SPREAD LOGIC (More particles for "More Smoke" request)
            int dustCount = (int)(10 + intensity * 30); 
            for (int i = 0; i < dustCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double speed = 0.15 + random.nextDouble() * 0.45 * intensity;
                
                Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                Vec3 tangent1 = normal.cross(new Vec3(1, 0, 0));
                if (tangent1.lengthSqr() < 0.01) tangent1 = normal.cross(new Vec3(0, 0, 1));
                tangent1 = tangent1.normalize();
                Vec3 tangent2 = normal.cross(tangent1).normalize();

                double vx = (Math.cos(angle) * tangent1.x + Math.sin(angle) * tangent2.x) * speed;
                double vy = (Math.cos(angle) * tangent1.y + Math.sin(angle) * tangent2.y) * speed;
                double vz = (Math.cos(angle) * tangent1.z + Math.sin(angle) * tangent2.z) * speed;
                
                vx += normal.x * 0.08 * intensity;
                vy += normal.y * 0.08 * intensity;
                vz += normal.z * 0.08 * intensity;

                worldLevel.addParticle(dev.devce.rocketnautics.registry.RocketParticles.JET_SMOKE.get(), 
                        hitPos.x + vx, hitPos.y + vy, hitPos.z + vz, 
                        vx, vy, vz);
            }
        }
    }

    protected void spawnSmokeParticles(BlockPos pos, Vector3d pDir, int visualPower, RandomSource random) {
        double smokeX = pos.getX() + 0.5 + pDir.x() * 2.5;
        double smokeY = pos.getY() + 0.5 + pDir.y() * 2.5;
        double smokeZ = pos.getZ() + 0.5 + pDir.z() * 2.5;

        for (int i = 0; i < (1 + visualPower / 10); i++) {
            double speedX = pDir.x() * (0.8 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.8;
            double speedY = pDir.y() * (0.8 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.8;
            double speedZ = pDir.z() * (0.8 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.8;
            getLevel().addParticle(RocketParticles.JET_SMOKE.get(), smokeX, smokeY, smokeZ, speedX, speedY, speedZ);
        }
    }

    public Vector3d getParticleDirection() {
        Direction nozzle = getThrustDirection();
        return new Vector3d(nozzle.getStepX(), nozzle.getStepY(), nozzle.getStepZ());
    }

    public float getRenderPower() {
        return Math.min(1.0f, getCurrentPower() / 50.0f);
    }

    protected float getVisualBoost() {
        return 1.0f;
    }

    protected void applyThrusterEffects(Level level, BlockPos pos) {
        Direction nozzle = getThrustDirection();
        int power = getCurrentPower();
        int visualPower = (int) (power * VISUAL_POWER_MULTIPLIER);
        double reach = 1.0 + (visualPower / 5.0);

        Vec3 start = Vec3.atCenterOf(pos).add(nozzle.getStepX() * 0.5, nozzle.getStepY() * 0.5, nozzle.getStepZ() * 0.5);
        Vec3 end = start.add(nozzle.getStepX() * reach, nozzle.getStepY() * reach, nozzle.getStepZ() * reach);
        AABB damageArea = new AABB(start, end).inflate(0.5);

        applyEntityPush(level, nozzle, visualPower, damageArea);

        if (!level.isClientSide && level.getGameTime() % 10 == 0) {
            applyEnvironmentHeat(level, pos, nozzle, visualPower, damageArea);
        }
    }

    private void applyEntityPush(Level level, Direction nozzle, int visualPower, AABB damageArea) {
        List<LivingEntity> affectedEntities = level.getEntitiesOfClass(LivingEntity.class, damageArea);
        double pushStrength = visualPower * PUSH_STRENGTH_MULTIPLIER;
        
        affectedEntities.forEach(entity -> {
            if (entity.isAlive()) {
                entity.push(nozzle.getStepX() * pushStrength, nozzle.getStepY() * pushStrength, nozzle.getStepZ() * pushStrength);
                if (!level.isClientSide) {
                    entity.hurtMarked = true;
                }
            }
        });
    }

    private void applyEnvironmentHeat(Level level, BlockPos pos, Direction nozzle, int visualPower, AABB damageArea) {
        // Damage entities
        List<LivingEntity> affectedEntities = level.getEntitiesOfClass(LivingEntity.class, damageArea);
        float damage = visualPower * DAMAGE_PER_POWER;
        
        affectedEntities.forEach(entity -> {
            if (entity.isAlive()) {
                entity.hurt(level.damageSources().lava(), damage);
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 40);
            }
        });

        // Melt blocks
        for (int dist = 1; dist <= 3; dist++) {
            BlockPos targetPos = pos.relative(nozzle, dist);
            BlockState targetState = level.getBlockState(targetPos);

            if (targetState.isAir()) continue;

            float hardness = targetState.getDestroySpeed(level, targetPos);
            if (hardness < 0 || hardness > 10.0f) break;

            if (level.random.nextInt(100) < (visualPower * 2)) {
                if (targetState.is(Blocks.LAVA)) break;

                if (targetState.is(Blocks.MAGMA_BLOCK)) {
                    level.setBlock(targetPos, Blocks.LAVA.defaultBlockState(), 3);
                } else {
                    level.setBlock(targetPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                }
            }

            if (targetState.isCollisionShapeFullBlock(level, targetPos)) break;
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel serverSubLevel, RigidBodyHandle handle, double deltaTime) {
        if (!isActive()) return;

        Direction facing = getThrustDirection();
        Direction pushDirection = facing.getOpposite();
        double currentThrust = getCurrentPower() * THRUST_FORCE_MULTIPLIER;

        Vector3d thrustVector = getPhysicsThrustVector(pushDirection, currentThrust);
        Vector3d blockCenter = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        
        handle.applyImpulseAtPoint(blockCenter, thrustVector.mul(deltaTime));
    }

    protected Vector3d getPhysicsThrustVector(Direction pushDirection, double thrustMagnitude) {
        return new Vector3d(
                pushDirection.getStepX() * thrustMagnitude,
                pushDirection.getStepY() * thrustMagnitude,
                pushDirection.getStepZ() * thrustMagnitude
        );
    }

    @Override
    public void remove() {
        super.remove();
        if (level != null && level.isClientSide) {
            stopSound();
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void updateSound(boolean active) {
        if (!active) {
            stopSound();
            return;
        }

        if (soundInstance == null) {
            startSound();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void startSound() {
        Minecraft mc = Minecraft.getInstance();
        ThrusterSoundInstance instance = new ThrusterSoundInstance(this);
        mc.getSoundManager().play(instance);
        soundInstance = instance;
    }

    @OnlyIn(Dist.CLIENT)
    private void stopSound() {
        if (soundInstance instanceof ThrusterSoundInstance instance) {
            instance.stopSound();
            soundInstance = null;
        }
    }

    @Override
    protected void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Burning", currentlyBurning);
        tag.putInt("IgnitionTicks", ignitionTicks);
    }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        currentlyBurning = tag.getBoolean("Burning");
        ignitionTicks = tag.getInt("IgnitionTicks");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable(getBlockState().getBlock().getDescriptionId()).withStyle(ChatFormatting.GOLD)));
        
        tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.status")).append(": ")
                .append(isActive() ? Component.translatable("rocketnautics.goggles.active").withStyle(ChatFormatting.GREEN) : Component.translatable("rocketnautics.goggles.inactive").withStyle(ChatFormatting.RED)));
        
        int power = getCurrentPower();
        tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.thrust")).append(": ")
                .append(Component.literal(power * THRUST_FORCE_MULTIPLIER + " N").withStyle(ChatFormatting.GOLD)));

        return true;
    }
}
