package dev.devce.rocketnautics.content.blocks;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketParticles;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.List;

public class BoosterThrusterBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor, IThruster, com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    private static final long FUEL_SCAN_CACHE_TICKS = 5L;

    public ScrollValueBehaviour thrustPower;
    public int ignitionTicks = 0;

    public int getWarmupTime() {
        return 40;
    }

    @Override
    public ScrollValueBehaviour getThrustPower() {
        return thrustPower;
    }

    public BoosterThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.BOOSTER_THRUSTER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        thrustPower = new ScrollValueBehaviour(
                Component.translatable("gui.rocketnautics.thrust_power"), 
                this, 
                new CenteredSideValueBoxTransform((state, direction) -> direction != state.getValue(RocketThrusterBlock.FACING))
        );
        thrustPower.between(1, 50);
        thrustPower.withFormatter(v -> (v * 10) + " N");
        thrustPower.setValue(50);
        
        behaviours.add(thrustPower);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BoosterThrusterBlockEntity blockEntity) {
        boolean active = blockEntity.isActive();
        if (!level.isClientSide) {
            if (active != blockEntity.currentlyBurning) {
                blockEntity.currentlyBurning = active;
                blockEntity.sendData();
            }
        }

        blockEntity.tick();
        
        if (level.isClientSide()) {
            blockEntity.updateSound();
            
            if (active) {
                BlockPos fuelPos = blockEntity.findFuelPos();
                if (fuelPos != null) {
                    for (int i = 0; i < 2; i++) {
                        double fx = fuelPos.getX() + level.random.nextDouble();
                        double fy = fuelPos.getY() + level.random.nextDouble();
                        double fz = fuelPos.getZ() + level.random.nextDouble();
                        level.addParticle(ParticleTypes.FLAME, fx, fy, fz, 0, 0, 0);
                    }
                }

                Direction nozzle = blockEntity.getThrustDirection();
                double x = pos.getX() + 0.5 + nozzle.getStepX() * 0.7;
                double y = pos.getY() + 0.5 + nozzle.getStepY() * 0.7;
                double z = pos.getZ() + 0.5 + nozzle.getStepZ() * 0.7;

                RandomSource random = level.getRandom();
                int power = blockEntity.thrustPower.getValue();
                int visualPower = (int)(power * 2.85f);

                net.minecraft.world.phys.Vec3 start = new net.minecraft.world.phys.Vec3(x, y, z);
                double maxSearchDist = 15.0 + (visualPower / 10.0);
                net.minecraft.world.phys.Vec3 end = start.add(nozzle.getStepX() * maxSearchDist, nozzle.getStepY() * maxSearchDist, nozzle.getStepZ() * maxSearchDist);
                net.minecraft.world.phys.BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
                
                double hitDist = maxSearchDist;
                boolean hitBlock = false;
                net.minecraft.world.phys.Vec3 hitPos = end;
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    hitDist = start.distanceTo(hit.getLocation());
                    hitBlock = true;
                    hitPos = hit.getLocation();
                }

                int plumeCount = 1 + (visualPower / 5);
                float baseSpeedMult = 0.8f + (visualPower / 100.0f) * 1.2f;
                float maxSpeedMult = (float)(hitDist / 15.0);
                float actualSpeedMult = Math.min(baseSpeedMult, maxSpeedMult);
            
                for (int i = 0; i < plumeCount; i++) {
                    double speedX = nozzle.getStepX() * (1.5 + random.nextDouble() * 1.0) * 1.0f * actualSpeedMult + (random.nextDouble() - 0.5) * 0.2;
                    double speedY = nozzle.getStepY() * (1.5 + random.nextDouble() * 1.0) * 1.0f * actualSpeedMult + (random.nextDouble() - 0.5) * 0.2;
                    double speedZ = nozzle.getStepZ() * (1.5 + random.nextDouble() * 1.0) * 1.0f * actualSpeedMult + (random.nextDouble() - 0.5) * 0.2;
                    
                    var particle = (blockEntity.ignitionTicks < blockEntity.getWarmupTime()) ? 
                            RocketParticles.PLUME.get() : 
                            RocketParticles.BLUE_FLAME.get();
                    
                    level.addParticle(particle, x, y, z, speedX, speedY, speedZ);
                }
                
                if (hitBlock && random.nextFloat() < (visualPower / 50.0f)) {
                    for (int i = 0; i < (1 + visualPower / 5); i++) {
                        net.minecraft.world.phys.Vec3 normal = new net.minecraft.world.phys.Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
                        net.minecraft.world.phys.Vec3 randomDir = new net.minecraft.world.phys.Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
                        net.minecraft.world.phys.Vec3 spreadDir = randomDir.subtract(normal.scale(randomDir.dot(normal))).normalize();
                        double speed = 0.5 + random.nextDouble() * 1.5;
                        level.addParticle(RocketParticles.JET_SMOKE.get(), hitPos.x, hitPos.y, hitPos.z, spreadDir.x * speed, spreadDir.y * speed, spreadDir.z * speed);
                    }
                } else if (!hitBlock && random.nextFloat() < (visualPower / 100.0f)) {
                    double smokeX = x + nozzle.getStepX() * hitDist * 0.8;
                    double smokeY = y + nozzle.getStepY() * hitDist * 0.8;
                    double smokeZ = z + nozzle.getStepZ() * hitDist * 0.8;
                    
                    for (int i = 0; i < (1 + visualPower / 10); i++) {
                        double speedX = nozzle.getStepX() * (0.8 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.8;
                        double speedY = nozzle.getStepY() * (0.8 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.8;
                        double speedZ = nozzle.getStepZ() * (0.8 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.8;
                        level.addParticle(RocketParticles.JET_SMOKE.get(), smokeX, smokeY, smokeZ, speedX, speedY, speedZ);
                    }
                }
            }
        }
        
        if (active) {
            if (blockEntity.ignitionTicks < 100) blockEntity.ignitionTicks++;
        } else {
            if (blockEntity.ignitionTicks > 0) {
                blockEntity.ignitionTicks--;
            }
        }
        
        if (!level.isClientSide && active) {
            blockEntity.handleHeat();
            blockEntity.consumeFuel();
        }
    }

    private void handleHeat() {
        Direction nozzle = getThrustDirection();
        int power = thrustPower.getValue();
        int visualPower = (int)(power * 2.85f);
        double reach = 1.0 + (visualPower / 5.0);
        Vec3 start = Vec3.atCenterOf(worldPosition).add(nozzle.getStepX() * 0.5, nozzle.getStepY() * 0.5, nozzle.getStepZ() * 0.5);
        Vec3 end = start.add(nozzle.getStepX() * reach, nozzle.getStepY() * reach, nozzle.getStepZ() * reach);
        AABB damageArea = new AABB(start, end).inflate(0.5);

        List<LivingEntity> affectedEntities = level.getEntitiesOfClass(LivingEntity.class, damageArea);
        affectedEntities.forEach(entity -> {
            if (entity.isAlive()) {
                double pushStrength = (visualPower / 150.0);
                entity.push(nozzle.getStepX() * pushStrength, nozzle.getStepY() * pushStrength, nozzle.getStepZ() * pushStrength);
                if (!level.isClientSide) {
                    entity.hurtMarked = true;
                }
            }
        });

        if (!level.isClientSide && level.getGameTime() % 10 == 0) {
            for (int dist = 1; dist <= 3; dist++) {
                BlockPos targetPos = worldPosition.relative(nozzle, dist);
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

            affectedEntities.forEach(entity -> {
                if (entity.isAlive()) {
                    entity.hurt(level.damageSources().lava(), (float) (visualPower / 10.0));
                    entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 40);
                }
            });
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel serverSubLevel, RigidBodyHandle handle, double deltaTime) {
        if (!isActive()) return;
        Direction facing = getThrustDirection();
        Direction pushDirection = facing.getOpposite();
        double currentThrust = thrustPower.getValue() * 10.0;
        Vector3d thrustVector = new Vector3d(pushDirection.getStepX() * currentThrust, pushDirection.getStepY() * currentThrust, pushDirection.getStepZ() * currentThrust);
        
        Vector3d blockCenter = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        handle.applyImpulseAtPoint(blockCenter, thrustVector.mul(deltaTime));
    }

    @Override
    public void remove() {
        super.remove();
        if (level != null && level.isClientSide) {
            ClientLogic.stopSound(this);
        }
    }

    private Object soundInstance;

    private void updateSound() {
        if (level.isClientSide) {
            ClientLogic.updateSound(this);
        }
    }

    private static class ClientLogic {
        private static void updateSound(BoosterThrusterBlockEntity be) {
            if (!be.isActive()) {
                stopSound(be);
                return;
            }

            if (be.soundInstance == null) {
                startSound(be);
            }
        }

        private static void startSound(BoosterThrusterBlockEntity be) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            ThrusterSoundInstance instance = new ThrusterSoundInstance(be);
            mc.getSoundManager().play(instance);
            be.soundInstance = instance;
        }

        private static void stopSound(BoosterThrusterBlockEntity be) {
            if (be.soundInstance instanceof ThrusterSoundInstance instance) {
                instance.stopSound();
                be.soundInstance = null;
            }
        }
    }

    private boolean currentlyBurning = false;
    private boolean ignited = false;
    private boolean isSpent = false;
    private int fuelTicks = 0;
    private BlockPos cachedFuelPos = null;
    private long cachedFuelPosTick = Long.MIN_VALUE;
    private boolean fuelCacheValid = false;

    public boolean isActive() {
        if (level != null && level.isClientSide) return currentlyBurning;
        
        if (isSpent) return false;

        if (ignited && fuelTicks > 0) return true;

        if (!ignited) {
            boolean redstonePowered = getBlockState().getValue(BoosterThrusterBlock.POWERED);
            if (redstonePowered && hasSolidFuelBehind()) {
                ignited = true;
                consumeSolidFuelBlock();
                notifyUpdate();
                return true;
            }
            return false;
        }
        
        if (ignited && fuelTicks <= 0) {
            BlockPos nextFuel = findFuelPos();
            if (nextFuel != null) {
                consumeSolidFuelBlock();
                notifyUpdate();
                return true;
            } else {
                isSpent = true;
                notifyUpdate();
                return false;
            }
        }

        return false;
    }

    private boolean hasSolidFuelBehind() {
        return findFuelPos() != null;
    }

    private BlockPos findFuelPos() {
        if (level != null) {
            long gameTime = level.getGameTime();
            if (fuelCacheValid && gameTime - cachedFuelPosTick <= FUEL_SCAN_CACHE_TICKS) {
                return cachedFuelPos;
            }
        }

        BlockPos scannedFuelPos = scanFuelPos();
        cachedFuelPos = scannedFuelPos;
        fuelCacheValid = true;
        cachedFuelPosTick = level != null ? level.getGameTime() : Long.MIN_VALUE;
        return scannedFuelPos;
    }

    private BlockPos scanFuelPos() {
        Direction nozzle = getThrustDirection();
        Direction back = nozzle.getOpposite();
        
        Direction axis1;
        Direction axis2;
        
        if (back.getAxis() == Direction.Axis.Y) {
            axis1 = Direction.NORTH;
            axis2 = Direction.EAST;
        } else if (back.getAxis() == Direction.Axis.X) {
            axis1 = Direction.UP;
            axis2 = Direction.NORTH;
        } else {
            axis1 = Direction.UP;
            axis2 = Direction.EAST;
        }

        BlockPos furthestInChain = null;

        for (int dist = 1; dist <= 64; dist++) {
            BlockPos layerCenter = worldPosition.relative(back, dist);
            boolean foundInLayer = false;
            BlockPos lastFoundInLayer = null;
            
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    BlockPos fuelPos = layerCenter.relative(axis1, i).relative(axis2, j);
                    BlockState state = level.getBlockState(fuelPos);
                    
                    if (isBlockCombustible(state)) {
                        foundInLayer = true;
                        lastFoundInLayer = fuelPos;
                    }
                }
            }
            
            if (foundInLayer) {
                furthestInChain = lastFoundInLayer;
            } else {
                break;
            }
        }
        
        return furthestInChain;
    }

    private boolean isBlockCombustible(BlockState state) {
        return state.is(Blocks.COAL_BLOCK);
    }

    private void consumeSolidFuelBlock() {
        BlockPos fuelPos = findFuelPos();
        if (fuelPos != null) {
            level.setBlock(fuelPos, Blocks.AIR.defaultBlockState(), 3);
            fuelTicks = 200;
            invalidateFuelCache();
        }
    }

    private void invalidateFuelCache() {
        fuelCacheValid = false;
        cachedFuelPos = null;
        cachedFuelPosTick = Long.MIN_VALUE;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Burning", currentlyBurning);
        tag.putBoolean("Ignited", ignited);
        tag.putBoolean("Spent", isSpent);
        tag.putInt("FuelTicks", fuelTicks);
        tag.putInt("IgnitionTicks", ignitionTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        currentlyBurning = tag.getBoolean("Burning");
        ignited = tag.getBoolean("Ignited");
        isSpent = tag.getBoolean("Spent");
        fuelTicks = tag.getInt("FuelTicks");
        ignitionTicks = tag.getInt("IgnitionTicks");
        invalidateFuelCache();
    }

    private void consumeFuel() {
        if (fuelTicks > 0) {
            fuelTicks--;
        }
    }

    public Direction getThrustDirection() {
        return getBlockState().getValue(RocketThrusterBlock.FACING);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable(getBlockState().getBlock().getDescriptionId()).withStyle(net.minecraft.ChatFormatting.GOLD)));
        
        tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.status")).append(": ")
                .append(isActive() ? Component.translatable("rocketnautics.goggles.active").withStyle(net.minecraft.ChatFormatting.GREEN) : 
                        (isSpent ? Component.translatable("rocketnautics.goggles.spent").withStyle(net.minecraft.ChatFormatting.DARK_GRAY) : 
                                   Component.translatable("rocketnautics.goggles.inactive").withStyle(net.minecraft.ChatFormatting.RED))));
        
        int power = thrustPower.getValue();
        tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.thrust")).append(": ")
                .append(Component.literal(power * 10 + " N").withStyle(net.minecraft.ChatFormatting.GOLD)));
        
        if (ignited && fuelTicks > 0) {
            tooltip.add(Component.literal("  ").append(Component.translatable("rocketnautics.goggles.burn_time")).append(": ")
                    .append(Component.literal((fuelTicks / 20) + "s").withStyle(net.minecraft.ChatFormatting.AQUA)));
        }
        
        return true;
    }

    public boolean isIgnited() { return ignited; }
    public boolean isSpent() { return isSpent; }
    public int getFuelTicks() { return fuelTicks; }
}
