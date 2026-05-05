package dev.devce.rocketnautics.content.physics;

import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketTags;
import dev.devce.rocketnautics.content.blocks.RocketThrusterBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.Quaterniondc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles collision detection and catastrophic failure (explosions) for ships.
 * Monitors velocity changes to detect impacts, identifies fuel sources on ships,
 * and triggers synchronized explosions in both the ship's plot and the world.
 */
public class CollisionDamageHandler {

    private static final Map<UUID, Vector3d> lastVelocities = new HashMap<>();

    /**
     * Initializes the collision monitoring system.
     */
    public static void init() {
        SableEventPlatform.INSTANCE.onPhysicsTick((physicsSystem, timeStep) -> {
            if (!RocketConfig.SERVER.enableCollisionExplosion.get()) return;

            ServerLevel level = physicsSystem.getLevel();
            SubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) return;

            // Check every active ship for potential impacts
            for (SubLevel subLevel : container.getAllSubLevels()) {
                if (subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
                    RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);
                    if (handle != null) {
                        processCollision(serverSubLevel, handle, level);
                    }
                }
            }
        });
    }


    /**
     * Detects sudden changes in velocity (DeltaV) and determines if they represent a crash.
     */
    private static void processCollision(ServerSubLevel ship, RigidBodyHandle handle, ServerLevel level) {
        Vector3d currentVel = new Vector3d(handle.getLinearVelocity());
        Vector3d lastVel = lastVelocities.get(ship.getUniqueId());

        if (lastVel != null) {
            double deltaV = lastVel.distance(currentVel);
            double threshold = RocketConfig.SERVER.collisionExplosionThreshold.get();

            // If a significant velocity change occurred
            if (deltaV > threshold) {
                Vector3d impulse = new Vector3d(currentVel).sub(lastVel);
                double dot = impulse.dot(lastVel);
                
                // 1. Was the ship already moving significantly?
                boolean wasMoving = lastVel.length() > 5.0;
                
                // 2. Unified Impact Detection:
                // We distinguish between a crash and a legitimate "push" (like lifting off or being towed).
                boolean isImpact;
                if (wasMoving) {
                    // If moving: impact is a loss of speed or a sharp change in direction.
                    boolean isSpeedLoss = currentVel.lengthSquared() < lastVel.lengthSquared() * 0.95;
                    boolean isDirectionChange = dot < -1.0;
                    isImpact = isSpeedLoss || isDirectionChange;
                } else {
                    // From rest: Only downward "slams" count (crash during landing/fall).
                    // This prevents accidental explosions when starting engines.
                    boolean isDownwardSlam = impulse.y < -2.0;
                    isImpact = isDownwardSlam || deltaV > threshold * 3.5; 
                }

                // Final confirmation: check if the ship's bounding box is actually touching something.
                if (isImpact && isActuallyColliding(ship, level)) {
                    RocketNautics.LOGGER.info("Ship {} impact detected! DeltaV: {} m/s, Dot: {}, Moving: {}", ship.getUniqueId(), deltaV, dot, wasMoving);
                    triggerFuelExplosions(ship, level);
                }
            }
        }

        lastVelocities.put(ship.getUniqueId(), currentVel);
    }

    private static boolean isActuallyColliding(ServerSubLevel ship, ServerLevel level) {
        // Convert JOML BoundingBox3dc to Minecraft AABB
        var jomlBox = ship.boundingBox();
        net.minecraft.world.phys.AABB shipBounds = new net.minecraft.world.phys.AABB(
            jomlBox.minX(), jomlBox.minY(), jomlBox.minZ(),
            jomlBox.maxX(), jomlBox.maxY(), jomlBox.maxZ()
        );
        
        // Expand slightly to catch ground contact
        net.minecraft.world.phys.AABB checkBounds = shipBounds.inflate(0.5);

        if (!level.noCollision(checkBounds)) {
            return true;
        }

        // 2. Check for contact with other ships
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null) {
            for (SubLevel other : container.getAllSubLevels()) {
                if (other != ship && !other.isRemoved()) {
                    var otherJoml = other.boundingBox();
                    net.minecraft.world.phys.AABB otherBounds = new net.minecraft.world.phys.AABB(
                        otherJoml.minX(), otherJoml.minY(), otherJoml.minZ(),
                        otherJoml.maxX(), otherJoml.maxY(), otherJoml.maxZ()
                    );
                    if (shipBounds.intersects(otherBounds)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Triggers fuel-based explosions for all blocks on the ship containing flammable fluids.
     * This creates a chain reaction effect.
     */
    private static void triggerFuelExplosions(ServerSubLevel ship, ServerLevel level) {
        net.minecraft.world.level.ChunkPos minChunk = ship.getPlot().getChunkMin();
        net.minecraft.world.level.ChunkPos maxChunk = ship.getPlot().getChunkMax();

        java.util.List<ExplosionPoint> potentialExplosions = new java.util.ArrayList<>();

        // 1. Scan ship plot for all fuel-carrying blocks (thrusters, tanks, etc.)
        for (int cx = minChunk.x; cx <= maxChunk.x; cx++) {
            for (int cz = minChunk.z; cz <= maxChunk.z; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.isRemoved()) continue;

                    float power = 0;
                    BlockPos pos = be.getBlockPos();

                    // Specific check for thrusters and generic check for IFluidHandler
                    if (be instanceof RocketThrusterBlockEntity thruster) {
                        if (thruster.fuelTank.getFluidAmount() > 0 && isRocketFuel(thruster.fuelTank.getFluid())) {
                            power = 4.0f + (thruster.fuelTank.getFluidAmount() / 250.0f);
                        }
                    } else {
                        var fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, be.getBlockState(), be, null);
                        if (fluidHandler != null) {
                            for (int i = 0; i < fluidHandler.getTanks(); i++) {
                                FluidStack stack = fluidHandler.getFluidInTank(i);
                                if (!stack.isEmpty() && isRocketFuel(stack)) {
                                    power = Math.max(power, 3.0f + (stack.getAmount() / 1000.0f));
                                }
                            }
                        }
                    }

                    if (power > 0) {
                        potentialExplosions.add(new ExplosionPoint(pos.immutable(), power));
                    }
                }
            }
        }

        if (potentialExplosions.isEmpty()) return;

        // 2. Shuffle and limit the number of explosions for performance stability
        java.util.Collections.shuffle(potentialExplosions);
        int maxExplosions = RocketConfig.SERVER.maxExplosionsPerShip.get();
        double maxPower = RocketConfig.SERVER.maxExplosionPower.get();
        
        Pose3d pose = ship.logicalPose();
        int count = 0;
        
        // Plot origin is needed to transform plot-local coordinates to world coordinates
        net.minecraft.world.level.ChunkPos plotMin = ship.getPlot().getChunkMin();
        double originX = plotMin.x * 16.0;
        double originZ = plotMin.z * 16.0;

        for (ExplosionPoint p : potentialExplosions) {
            if (count++ >= maxExplosions) break;

            float power = (float) Math.min(maxPower, p.power);
            
            // Convert coordinate systems: PLOT -> LOCAL -> WORLD
            Vector3d localPos = new Vector3d(
                p.pos.getX() + 0.5 - originX, 
                p.pos.getY() + 0.5, 
                p.pos.getZ() + 0.5 - originZ
            );
            
            Vector3d worldPos = new Vector3d(localPos);
            worldPos.rotate(pose.orientation()); 
            worldPos.add(pose.position());       
            
            // Explosion 1: In the Ship Plot (destroys the ship itself)
            level.explode(null, p.pos.getX() + 0.5, p.pos.getY() + 0.5, p.pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);
            
            // Explosion 2: In the World (destroys the environment/terrain)
            level.explode(null, worldPos.x, worldPos.y, worldPos.z, power * 0.8f, Level.ExplosionInteraction.BLOCK);
            
            // Visuals and environmental side-effects
            createEnvironmentalDamage(level, new BlockPos((int)worldPos.x, (int)worldPos.y, (int)worldPos.z), power);
            spawnCustomExplosionParticles(level, worldPos, power);
        }
    }

    private static void createEnvironmentalDamage(ServerLevel level, BlockPos center, float power) {
        int radius = (int) (power * 0.4f);
        radius = Math.min(radius, 8); // Safety cap
        
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (p.distSqr(center) > radius * radius) continue;
            if (level.random.nextFloat() > 0.35f) continue;

            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(p);
            if (state.isAir()) {
                if (level.getBlockState(p.below()).isSolidRender(level, p.below())) {
                    level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                }
            } else if (state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.tags.BlockTags.SAND)) {
                float roll = level.random.nextFloat();
                if (roll < 0.2f) level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState());
                else if (roll < 0.5f) level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState());
            }
        }
    }

    private record ExplosionPoint(BlockPos pos, float power) {}

    private static void spawnCustomExplosionParticles(ServerLevel level, Vector3d pos, float power) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        // Balanced density to prevent packet overflow
        int density = (int) (power * 50);
        float spread = power * 0.4f;

        var plasma = dev.devce.rocketnautics.registry.RocketParticles.PLASMA.get();
        var plume = dev.devce.rocketnautics.registry.RocketParticles.PLUME.get();
        var smoke = net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
        var jetSmoke = dev.devce.rocketnautics.registry.RocketParticles.JET_SMOKE.get();
        var campfire = net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE;
        var poof = net.minecraft.core.particles.ParticleTypes.POOF;
        var flash = net.minecraft.core.particles.ParticleTypes.FLASH;
        var explosion = net.minecraft.core.particles.ParticleTypes.EXPLOSION;

        // Correct 11-argument overload: sendParticles(ServerPlayer, T, boolean force, double x, y, z, int count, double dx, dy, dz, double speed)
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            level.sendParticles(player, flash, true, x, y, z, 5, 0.1, 0.1, 0.1, 0.0);
            level.sendParticles(player, plasma, true, x, y, z, density, spread * 0.2, spread * 0.2, spread * 0.2, 0.5);
            level.sendParticles(player, plume, true, x, y, z, density * 2, spread * 0.4, spread * 0.4, spread * 0.4, 0.3);
            
            level.sendParticles(player, smoke, true, x, y, z, density, spread, spread, spread, 0.05);
            level.sendParticles(player, jetSmoke, true, x, y, z, density, spread * 0.7, spread * 0.7, spread * 0.7, 0.15);
            level.sendParticles(player, campfire, true, x, y, z, density / 2, spread * 1.5, 1.0, spread * 1.5, 0.02);
    
            level.sendParticles(player, poof, true, x, y, z, density / 2, spread * 0.5, 0.1, spread * 0.5, 0.7);
            level.sendParticles(player, explosion, true, x, y, z, 3, 1.0, 0.5, 1.0, 0.1);
        }
    }

    private static boolean isRocketFuel(FluidStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getFluid().isSame(net.minecraft.world.level.material.Fluids.LAVA)) return true;
        if (stack.is(RocketTags.Fluids.ROCKET_FUEL)) return true;

        String id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
        return id.contains("tfmg") && (id.contains("kerosene") || id.contains("diesel") || id.contains("gasoline") || id.contains("fuel_oil") || id.contains("lpg"));
    }
}
