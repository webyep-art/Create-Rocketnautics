package dev.devce.rocketnautics.content.blocks.rope;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BlockEntity for MultiRopeHubBlock.
 */
public class MultiRopeHubBlockEntity extends SmartBlockEntity implements RopeStrandHolderBlockEntity {

    private static final Field ATTACHED_ROPE_ID_FIELD;
    private static final Field STRAND_OWNER_FIELD;
    private static final Field OWNED_SERVER_STRAND_FIELD;
    private static final java.lang.reflect.Method TICK_STRAND_TRACKING_PLAYERS_METHOD;

    static {
        try {
            ATTACHED_ROPE_ID_FIELD = RopeStrandHolderBehavior.class.getDeclaredField("attachedRopeID");
            ATTACHED_ROPE_ID_FIELD.setAccessible(true);
            STRAND_OWNER_FIELD = RopeStrandHolderBehavior.class.getDeclaredField("strandOwner");
            STRAND_OWNER_FIELD.setAccessible(true);
            
            OWNED_SERVER_STRAND_FIELD = RopeStrandHolderBehavior.class.getDeclaredField("ownedServerStrand");
            OWNED_SERVER_STRAND_FIELD.setAccessible(true);
            
            TICK_STRAND_TRACKING_PLAYERS_METHOD = RopeStrandHolderBehavior.class.getDeclaredMethod("tickStrandTrackingPlayers");
            TICK_STRAND_TRACKING_PLAYERS_METHOD.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException("[MultiRopeHub] Cannot access RopeStrandHolderBehavior fields/methods", e);
        }
    }

    private DispatchHolder dispatch;
    private final List<CompoundTag> savedStrandsToLoad = new ArrayList<>();
    private BlockPos savedOldPos = null;

    public MultiRopeHubBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        this.dispatch = new DispatchHolder(this);
        behaviours.add(this.dispatch);
    }

    @Override
    public void notifyUpdate() {
        super.notifyUpdate();
        if (dispatch == null) return;
        try {
            final boolean isOwner = (boolean) STRAND_OWNER_FIELD.get(dispatch);
            if (!isOwner) {
                // End target — clear so the next rope can connect
                ATTACHED_ROPE_ID_FIELD.set(dispatch, null);
            } else {
                // Source owner — ALSO clear so this block can act as source multiple times!
                ATTACHED_ROPE_ID_FIELD.set(dispatch, null);
                STRAND_OWNER_FIELD.set(dispatch, false);
            }
        } catch (IllegalAccessException ignored) {}
    }

    // ── Saving/Loading multiple ropes for Schematics and Ships ────────────────

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (clientPacket) return;

        // When saved to a schematic or ship, we manually serialize ALL strands 
        // that start at this block. DispatchHolder won't save them because we clear the fields.
        if (level != null && !level.isClientSide) {
            final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
            if (manager != null) {
                ListTag strandsList = new ListTag();
                for (final ServerRopeStrand strand : manager.getAllStrands()) {
                    final RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
                    if (start != null && worldPosition.equals(start.blockAttachment())) {
                        ServerRopeStrand.CODEC.encodeStart(NbtOps.INSTANCE, strand)
                                .result()
                                .ifPresent(tag -> strandsList.add(tag));
                    }
                }
                if (!strandsList.isEmpty()) {
                    compound.put("HubOwnedStrands", strandsList);
                    compound.putLong("HubOldPos", worldPosition.asLong()); // Save our pos to calc offsets later
                }
            }
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket) return;

        savedStrandsToLoad.clear();
        savedOldPos = null;
        if (compound.contains("HubOwnedStrands")) {
            ListTag list = compound.getList("HubOwnedStrands", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                savedStrandsToLoad.add(list.getCompound(i));
            }
            if (compound.contains("HubOldPos")) {
                savedOldPos = BlockPos.of(compound.getLong("HubOldPos"));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level != null && !level.isClientSide) {
            final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
            if (manager != null) {
                // Manually tick tracking players for ALL strands starting here,
                // so the client can see them!
                if (dispatch != null) {
                    for (final ServerRopeStrand strand : manager.getAllStrands()) {
                        if (!strand.isActive()) continue;
                        
                        final RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
                        if (start != null && worldPosition.equals(start.blockAttachment())) {
                            try {
                                OWNED_SERVER_STRAND_FIELD.set(dispatch, strand);
                                TICK_STRAND_TRACKING_PLAYERS_METHOD.invoke(dispatch);
                            } catch (Exception ignored) {}
                        }
                    }
                    try {
                        OWNED_SERVER_STRAND_FIELD.set(dispatch, null);
                    } catch (Exception ignored) {}
                }

                // Restore saved ropes on the first server tick after loading
                if (!savedStrandsToLoad.isEmpty()) {
                    // Calculate position shift (caused by ShipCopyPasteCommand moving the SubLevel plot)
                    BlockPos offset = BlockPos.ZERO;
                    if (savedOldPos != null && !worldPosition.equals(savedOldPos)) {
                        offset = worldPosition.subtract(savedOldPos);
                    }

                    SubLevel currentSubLevel = Sable.HELPER.getContaining(level, worldPosition);
                    UUID currentSubLevelId = currentSubLevel != null ? currentSubLevel.getUniqueId() : null;

                    for (CompoundTag tag : savedStrandsToLoad) {
                        final BlockPos finalOffset = offset;
                        ServerRopeStrand.CODEC.parse(NbtOps.INSTANCE, tag)
                                .result()
                                .ifPresent(strand -> {
                                    // Important: when placing a schematic multiple times, 
                                    // we must avoid UUID collisions. Generate a fresh UUID.
                                    ServerRopeStrand newStrand = new ServerRopeStrand(UUID.randomUUID(), strand.getPoints());
                                    newStrand.updateFirstSegmentExtension(strand.getExtension());
                                    
                                    // Re-apply attachments with calculated offset and new SubLevel ID!
                                    RopeAttachment oldStart = strand.getAttachment(RopeAttachmentPoint.START);
                                    RopeAttachment oldEnd = strand.getAttachment(RopeAttachmentPoint.END);
                                    
                                    if (oldStart != null) {
                                        BlockPos newPos = oldStart.blockAttachment().offset(finalOffset);
                                        RopeAttachment newStart = new RopeAttachment(RopeAttachmentPoint.START, currentSubLevelId, newPos);
                                        newStrand.addAttachment((ServerLevel) level, RopeAttachmentPoint.START, newStart);
                                    }
                                    if (oldEnd != null) {
                                        BlockPos newPos = oldEnd.blockAttachment().offset(finalOffset);
                                        RopeAttachment newEnd = new RopeAttachment(RopeAttachmentPoint.END, currentSubLevelId, newPos);
                                        newStrand.addAttachment((ServerLevel) level, RopeAttachmentPoint.END, newEnd);
                                    }
                                    manager.addStrand(newStrand);
                                });
                    }
                    savedStrandsToLoad.clear();
                }
            }
        }
    }

    // ── getBehaviour ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntityBehaviour> T getBehaviour(final BehaviourType<T> type) {
        if (type == RopeStrandHolderBehavior.TYPE && dispatch != null) {
            return (T) dispatch;
        }
        return super.getBehaviour(type);
    }

    // ── Movement (afterMove called from MultiRopeHubBlock) ────────────────────

    public void handleAfterMove(final ServerLevel serverLevel, final BlockPos oldPos, final BlockPos newPos) {
        final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(serverLevel);
        if (manager == null) return;

        final SubLevel newSubLevel = Sable.HELPER.getContaining(serverLevel, newPos);
        final UUID newSubLevelId = newSubLevel != null ? newSubLevel.getUniqueId() : null;

        for (final ServerRopeStrand strand : manager.getAllStrands()) {
            final RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
            if (start != null && oldPos.equals(start.blockAttachment())) {
                strand.addAttachment(serverLevel, RopeAttachmentPoint.START,
                        new RopeAttachment(RopeAttachmentPoint.START, newSubLevelId, newPos));
                strand.getTrackingPlayers().clear();
            }

            final RopeAttachment end = strand.getAttachment(RopeAttachmentPoint.END);
            if (end != null && oldPos.equals(end.blockAttachment())) {
                strand.addAttachment(serverLevel, RopeAttachmentPoint.END,
                        new RopeAttachment(RopeAttachmentPoint.END, newSubLevelId, newPos));
                strand.getTrackingPlayers().clear();
            }
        }
    }

    // ── Shearing ──────────────────────────────────────────────────────────────

    public ItemInteractionResult shearLastRope(final ServerPlayer player, final BlockPos pos) {
        if (level == null || level.isClientSide) return ItemInteractionResult.FAIL;

        final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        if (manager == null) return ItemInteractionResult.FAIL;

        ServerRopeStrand targetStrand = null;
        // Destroy the most recently connected END or START strand at this block
        for (final ServerRopeStrand strand : manager.getAllStrands()) {
            final RopeAttachment end = strand.getAttachment(RopeAttachmentPoint.END);
            final RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
            
            if ((end != null && pos.equals(end.blockAttachment())) ||
                (start != null && pos.equals(start.blockAttachment()))) {
                targetStrand = strand;
            }
        }
        
        if (targetStrand != null) {
            manager.removeStrand(targetStrand.getUUID());
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.FAIL;
    }

    // ── RopeStrandHolderBlockEntity ───────────────────────────────────────────

    @Override
    public RopeStrandHolderBehavior getBehavior() {
        return dispatch;
    }

    @Override
    public Vec3 getAttachmentPoint(final BlockPos pos, final BlockState state) {
        final Direction facing = state.getValue(RopeConnectorBlock.FACING);
        final double offset = -3.0 / 16.0;
        return pos.getCenter().add(
                facing.getStepX() * offset,
                facing.getStepY() * offset,
                facing.getStepZ() * offset);
    }

    @Override
    public Vec3 getVisualAttachmentPoint(final BlockPos pos, final BlockState state) {
        final Direction facing = state.getValue(RopeConnectorBlock.FACING);
        final double offset = -4.0 / 16.0;
        return pos.getCenter().add(
                facing.getStepX() * offset,
                facing.getStepY() * offset,
                facing.getStepZ() * offset);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(64);
    }

    // ── DispatchHolder ────────────────────────────────────────────────────────

    private class DispatchHolder extends RopeStrandHolderBehavior {

        DispatchHolder(final SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean isAttached() {
            return false;
        }

        @Override
        public Vec3 getAttachmentPoint() {
            return MultiRopeHubBlockEntity.this.getAttachmentPoint(
                    blockEntity.getBlockPos(), blockEntity.getBlockState());
        }

        @Override
        public Vec3 getVisualAttachmentPoint() {
            return MultiRopeHubBlockEntity.this.getVisualAttachmentPoint(
                    blockEntity.getBlockPos(), blockEntity.getBlockState());
        }
    }
}
