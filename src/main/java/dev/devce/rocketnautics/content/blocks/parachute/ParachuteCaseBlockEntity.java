package dev.devce.rocketnautics.content.blocks.parachute;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;

public class ParachuteCaseBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor {

    private ItemStack parachute = ItemStack.EMPTY;
    private boolean isFalling = false; // Synced to client to control canopy dropping
    public boolean isFalling() { return isFalling; }

    // ── Client-side canopy animation ──────────────────────────────────────────
    public float canopyY = 8.0f;
    public float canopyVelocity = 0.0f;

    // ── Client-side verlet physics points [rope][point][xyz_and_prev] ────────
    private float[][][] physicsPoints = null;

    public ParachuteCaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ParachuteCaseBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.PARACHUTE_CASE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public long lastRenderTime = 0;

    @Override
    public void tick() {
        super.tick();
        // Animation is now handled in ParachuteRenderer to guarantee it runs smoothly every frame
    }

    // ── Verlet physics API (called from renderer, client-thread only) ─────────

    public float[][][] getOrInitPhysicsPoints(org.joml.Vector3f[] cornersLocal, int segs) {
        if (physicsPoints == null) {
            physicsPoints = new float[4][segs + 1][6];
            for (int r = 0; r < 4; r++) {
                float bx = 0.5f, by = 1.0f, bz = 0.5f;
                float tx = cornersLocal[r].x(), ty = cornersLocal[r].y(), tz = cornersLocal[r].z();
                for (int i = 0; i <= segs; i++) {
                    float t = (float) i / segs;
                    float x = bx + (tx - bx) * t;
                    float y = by + (ty - by) * t;
                    float z = bz + (tz - bz) * t;
                    physicsPoints[r][i][0] = x; physicsPoints[r][i][1] = y; physicsPoints[r][i][2] = z;
                    physicsPoints[r][i][3] = x; physicsPoints[r][i][4] = y; physicsPoints[r][i][5] = z;
                }
            }
        }
        return physicsPoints;
    }

    public void stepPhysics(float gravity, float damping, org.joml.Vector3f[] cornersLocal, int segs) {
        if (physicsPoints == null) return;
        
        // Hardcode rest length based on fully deployed state (Y=8.0)
        // length = sqrt(2^2 + 8^2 + 2^2) = sqrt(72) = 8.485f
        float restLen = 8.48528f / segs;

        for (int r = 0; r < 4; r++) {
            float[][] pts = physicsPoints[r];
            int last = segs;
            float bx = 0.5f, by = 1.0f, bz = 0.5f;
            float tx = cornersLocal[r].x(), ty = cornersLocal[r].y(), tz = cornersLocal[r].z();

            // Damped verlet integration
            for (int i = 1; i < last; i++) {
                float cx = pts[i][0], cy = pts[i][1], cz = pts[i][2];
                float px = pts[i][3], py = pts[i][4], pz = pts[i][5];

                float nx = cx + (cx - px) * damping;
                float ny = cy + (cy - py) * damping + gravity;
                float nz = cz + (cz - pz) * damping;

                pts[i][3] = cx; pts[i][4] = cy; pts[i][5] = cz;
                pts[i][0] = nx; pts[i][1] = ny; pts[i][2] = nz;
            }

            // Constraint relaxation
            for (int iter = 0; iter < 15; iter++) {
                pts[0][0]=bx; pts[0][1]=by; pts[0][2]=bz;
                pts[last][0]=tx; pts[last][1]=ty; pts[last][2]=tz;

                for (int i = 0; i < last; i++) {
                    float[] a = pts[i], b = pts[i + 1];
                    float ex = b[0]-a[0], ey = b[1]-a[1], ez = b[2]-a[2];
                    float dist = (float) Math.sqrt(ex*ex + ey*ey + ez*ez);
                    if (dist < 1e-6f) continue;
                    float c = (dist - restLen) / dist * 0.5f;
                    float corX = ex*c, corY = ey*c, corZ = ez*c;
                    if (i > 0)    { a[0]+=corX; a[1]+=corY; a[2]+=corZ; }
                    if (i+1<last) { b[0]-=corX; b[1]-=corY; b[2]-=corZ; }
                }
                
                // Floor collision so ropes pile up on the case instead of clipping through
                for (int i = 1; i < last; i++) {
                    if (pts[i][1] < 1.0f) pts[i][1] = 1.0f;
                }

                pts[0][0]=bx; pts[0][1]=by; pts[0][2]=bz;
                pts[last][0]=tx; pts[last][1]=ty; pts[last][2]=tz;
            }
        }
    }

    public void resetPhysics() {
        physicsPoints = null;
    }

    // ── Parachute item accessors ──────────────────────────────────────────────

    public ItemStack getParachute() { return parachute; }

    public void setParachute(ItemStack stack) {
        this.parachute = stack;
        setChanged();
        if (level != null) {
            level.setBlock(worldPosition,
                getBlockState().setValue(ParachuteCaseBlock.HAS_PARACHUTE, !stack.isEmpty()), 3);
        }
    }

    public boolean hasParachute() { return !parachute.isEmpty(); }

    // ── Physics tick (server-side Sable drag) ─────────────────────────────────

    public boolean hasLanded = false;
    public float worldGroundY = -100.0f; // Track ground level for rendering
    public float minLocalY = -20.0f;    // Minimum local Y to stay above ground

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        BlockState state = getBlockState();
        if (!state.hasProperty(ParachuteCaseBlock.OPEN) || !state.getValue(ParachuteCaseBlock.OPEN)) {
            this.hasLanded = false; 
            return;
        }
        if (!hasParachute()) return;

        Vector3d vel   = new Vector3d(handle.getLinearVelocity());
        double   speed = vel.length();
        
        // Height check to allow re-usability (reset hasLanded if high enough)
        Vector3d worldPos = subLevel.logicalPose().position();
        double groundY = subLevel.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)worldPos.x, (int)worldPos.z);
        double height = worldPos.y - groundY;
        
        // Update minLocalY for visual ground clamping
        this.minLocalY = (float)(groundY - worldPos.y);

        if (height > 25.0) {
            this.hasLanded = false;
        }

        // Falling if vertical velocity is downward and speed is significant
        boolean nowFalling = (vel.y() < -0.1) && (speed > 0.05);
        
        // If we were falling and now we stopped while near ground, we have landed.
        if (this.isFalling && !nowFalling && height < 15.0) {
            this.hasLanded = true;
            this.worldGroundY = (float) groundY;
            setChanged();
            subLevel.getLevel().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        
        // Lock to collapsed state if landed
        if (this.hasLanded) {
            nowFalling = false;
        }

        if (this.isFalling != nowFalling) {
            this.isFalling = nowFalling;
            setChanged();
            subLevel.getLevel().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        // --- Interactive Breaking Logic ---
        if (this.isFalling && this.canopyY > 7.8f) {
            BlockPos canopyPos = worldPosition.above(8);
            Level shipLevel = subLevel.getLevel();
            if (shipLevel.getBlockState(canopyPos).isAir()) {
                // If the canopy was already placed and now it's air, it was broken!
                if (shipLevel.getGameTime() % 10 == 0) { // Slight delay to allow placement
                     this.destroyParachute();
                     return;
                }
            } else if (shipLevel.getBlockState(canopyPos).is(net.minecraft.world.level.block.Blocks.AIR)) {
                 // redundant check
            } else if (!shipLevel.getBlockState(canopyPos).is(net.minecraft.world.level.block.Blocks.WHITE_WOOL)) {
                // Place wool if not there yet
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockPos p = canopyPos.offset(x, 0, z);
                        if (shipLevel.getBlockState(p).isAir()) {
                            shipLevel.setBlock(p, net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        if (speed < 0.05) return;

        // Reduce drag so the ship falls a bit faster (was 8.0)
        double drag  = 2.0;
        double mass  = subLevel.getMassTracker().getMass();
        double force = Math.min(speed * drag * mass, mass * 4.0);

        Vector3d impulse = new Vector3d(vel).normalize().negate().mul(force * dt);
        Quaterniond ori  = subLevel.logicalPose().orientation();
        Vector3d local   = ori.transformInverse(impulse, new Vector3d());
        Vector3d center  = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        handle.applyImpulseAtPoint(center, local);
    }

    private void destroyParachute() {
        if (parachute.isEmpty()) return;
        
        // Drop composite parts
        Level world = this.level;
        if (world != null) {
            BlockPos p = worldPosition;
            net.minecraft.world.Containers.dropItemStack(world, p.getX(), p.getY(), p.getZ(), new ItemStack(net.minecraft.world.level.block.Blocks.WHITE_WOOL, 12));
            net.minecraft.world.Containers.dropItemStack(world, p.getX(), p.getY(), p.getZ(), new ItemStack(net.minecraft.world.item.Items.LEAD, 4));
        }
        
        this.parachute = ItemStack.EMPTY;
        this.isFalling = false;
        this.hasLanded = false;
        setChanged();
        if (level != null) {
             level.setBlock(worldPosition, getBlockState().setValue(ParachuteCaseBlock.HAS_PARACHUTE, false).setValue(ParachuteCaseBlock.OPEN, false), 3);
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider reg, boolean clientPacket) {
        super.write(tag, reg, clientPacket);
        if (!parachute.isEmpty()) tag.put("Parachute", parachute.saveOptional(reg));
        tag.putBoolean("IsFalling", isFalling);
        tag.putBoolean("HasLanded", hasLanded);
        tag.putFloat("WorldGroundY", worldGroundY);
        tag.putFloat("MinLocalY", minLocalY);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider reg, boolean clientPacket) {
        super.read(tag, reg, clientPacket);
        parachute = tag.contains("Parachute")
            ? ItemStack.parseOptional(reg, tag.getCompound("Parachute"))
            : ItemStack.EMPTY;
        isFalling = tag.getBoolean("IsFalling");
        hasLanded = tag.getBoolean("HasLanded");
        worldGroundY = tag.getFloat("WorldGroundY");
        minLocalY = tag.getFloat("MinLocalY");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag tag = super.getUpdateTag(reg);
        write(tag, reg, true);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(3).expandTowards(0, 9, 0);
    }
}
