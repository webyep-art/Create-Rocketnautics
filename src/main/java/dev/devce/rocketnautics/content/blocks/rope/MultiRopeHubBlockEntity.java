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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BlockEntity for MultiRopeHubBlock.
 *
 * Design: one DispatchHolder per connected rope.
 * - currentDispatch  : accepts the next incoming rope connection (no strand yet)
 * - committedDispatches : one entry per successfully connected rope;
 *                         each keeps strandOwner=true + ownedServerStrand=theStrand
 *                         so the full RopeStrandHolderBehavior tick lifecycle runs.
 */
public class MultiRopeHubBlockEntity extends SmartBlockEntity implements RopeStrandHolderBlockEntity {

    // ── Reflection handles ────────────────────────────────────────────────────

    private static final Field ATTACHED_ROPE_ID_FIELD;
    private static final Field STRAND_OWNER_FIELD;
    private static final Field OWNED_SERVER_STRAND_FIELD;

    static {
        try {
            ATTACHED_ROPE_ID_FIELD = RopeStrandHolderBehavior.class.getDeclaredField("attachedRopeID");
            ATTACHED_ROPE_ID_FIELD.setAccessible(true);
            STRAND_OWNER_FIELD = RopeStrandHolderBehavior.class.getDeclaredField("strandOwner");
            STRAND_OWNER_FIELD.setAccessible(true);
            OWNED_SERVER_STRAND_FIELD = RopeStrandHolderBehavior.class.getDeclaredField("ownedServerStrand");
            OWNED_SERVER_STRAND_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("[MultiRopeHub] Cannot access RopeStrandHolderBehavior fields", e);
        }
    }

    // ── State ────────────────────────────────────────────────────────────────

    /** The behavior currently waiting for the next rope connection. */
    private DispatchHolder currentDispatch;

    /**
     * One committed DispatchHolder per active rope.
     * Each entry keeps strandOwner=true + ownedServerStrand=<strand>,
     * so tick() runs the full behavior lifecycle for that rope.
     * CopyOnWriteArrayList lets the tick loop remove dead entries safely.
     */
    private final CopyOnWriteArrayList<DispatchHolder> committedDispatches = new CopyOnWriteArrayList<>();

    private final List<CompoundTag> savedStrandsToLoad = new ArrayList<>();
    private BlockPos savedOldPos = null;

    // ── Constructor ──────────────────────────────────────────────────────────

    public MultiRopeHubBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    // ── Behaviours ───────────────────────────────────────────────────────────

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        this.currentDispatch = new DispatchHolder(this);
        // Committed dispatches are ticked manually in tick() — NOT added here.
        // Adding them would make SmartBlockEntity auto-tick them which would be
        // problematic since all DispatchHolders share the same BehaviourType key.
    }

    // ── notifyUpdate ─────────────────────────────────────────────────────────

    @Override
    public void notifyUpdate() {
        super.notifyUpdate();
        if (currentDispatch == null) return;
        try {
            final boolean isOwner = (boolean) STRAND_OWNER_FIELD.get(currentDispatch);
            if (isOwner) {
                // A rope was just successfully connected from this block.
                // Commit the current dispatch — it owns the strand and will tick it.
                committedDispatches.add(currentDispatch);
                // Create a fresh dispatch ready for the next rope.
                currentDispatch = new DispatchHolder(this);
            } else {
                // This block was the target end — just unlock for the next connection.
                ATTACHED_ROPE_ID_FIELD.set(currentDispatch, null);
            }
        } catch (IllegalAccessException ignored) {}
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) return;

        // Tick every committed dispatch.
        // CopyOnWriteArrayList snapshot prevents ConcurrentModificationException
        // if destroyRopeIfAttachmentBroken() modifies the underlying strands manager.
        for (final DispatchHolder committed : committedDispatches) {
            try {
                committed.tick();
            } catch (Exception e) {
                // Log so we can diagnose why the behavior tick fails (rope not visible issue)
                dev.devce.rocketnautics.RocketNautics.LOGGER.error(
                        "[MultiRopeHub] committed dispatch tick failed at {}", worldPosition, e);
            }

            try {
                if (OWNED_SERVER_STRAND_FIELD.get(committed) == null) {
                    committedDispatches.remove(committed);
                }
            } catch (IllegalAccessException ignored) {}
        }

        // Restore saved ropes on the first server tick after loading
        final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        if (manager != null && !savedStrandsToLoad.isEmpty()) {
            final BlockPos offset = (savedOldPos != null && !worldPosition.equals(savedOldPos))
                    ? worldPosition.subtract(savedOldPos) : BlockPos.ZERO;

            final SubLevel currentSubLevel = Sable.HELPER.getContaining(level, worldPosition);
            final UUID currentSubLevelId = currentSubLevel != null ? currentSubLevel.getUniqueId() : null;

            for (final CompoundTag tag : savedStrandsToLoad) {
                final BlockPos finalOffset = offset;
                ServerRopeStrand.CODEC.parse(NbtOps.INSTANCE, tag)
                        .result()
                        .ifPresent(strand -> {
                            final ServerRopeStrand newStrand =
                                    new ServerRopeStrand(UUID.randomUUID(), strand.getPoints());
                            newStrand.updateFirstSegmentExtension(strand.getExtension());

                            final RopeAttachment oldStart = strand.getAttachment(RopeAttachmentPoint.START);
                            final RopeAttachment oldEnd   = strand.getAttachment(RopeAttachmentPoint.END);

                            if (oldStart != null) {
                                final BlockPos newPos = oldStart.blockAttachment().offset(finalOffset);
                                newStrand.addAttachment((ServerLevel) level, RopeAttachmentPoint.START,
                                        new RopeAttachment(RopeAttachmentPoint.START, currentSubLevelId, newPos));
                            }
                            if (oldEnd != null) {
                                final BlockPos newPos = oldEnd.blockAttachment().offset(finalOffset);
                                newStrand.addAttachment((ServerLevel) level, RopeAttachmentPoint.END,
                                        new RopeAttachment(RopeAttachmentPoint.END, currentSubLevelId, newPos));
                            }
                            manager.addStrand(newStrand);
                        });
            }
            savedStrandsToLoad.clear();
        }
    }

    // ── Saving / Loading ──────────────────────────────────────────────────────

    @Override
    protected void write(final CompoundTag compound, final HolderLookup.Provider registries,
                         final boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (clientPacket) return;

        if (level != null && !level.isClientSide) {
            final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
            if (manager != null) {
                final ListTag strandsList = new ListTag();
                for (final ServerRopeStrand strand : manager.getAllStrands()) {
                    final RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
                    if (start != null && worldPosition.equals(start.blockAttachment())) {
                        ServerRopeStrand.CODEC.encodeStart(NbtOps.INSTANCE, strand)
                                .result()
                                .ifPresent(strandsList::add);
                    }
                }
                if (!strandsList.isEmpty()) {
                    compound.put("HubOwnedStrands", strandsList);
                    compound.putLong("HubOldPos", worldPosition.asLong());
                }
            }
        }
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries,
                        final boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket) return;

        savedStrandsToLoad.clear();
        savedOldPos = null;
        if (compound.contains("HubOwnedStrands")) {
            final ListTag list = compound.getList("HubOwnedStrands", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                savedStrandsToLoad.add(list.getCompound(i));
            }
            if (compound.contains("HubOldPos")) {
                savedOldPos = BlockPos.of(compound.getLong("HubOldPos"));
            }
        }
    }

    // ── getBehaviour override ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntityBehaviour> T getBehaviour(final BehaviourType<T> type) {
        if (type == RopeStrandHolderBehavior.TYPE) {
            // Always return currentDispatch so the RopeItem can always initiate a new connection.
            // makeUpdatePacket() on currentDispatch is safe because we override it below.
            return (T) currentDispatch;
        }
        return super.getBehaviour(type);
    }

    // ── Movement ──────────────────────────────────────────────────────────────

    public void handleAfterMove(final ServerLevel serverLevel, final BlockPos oldPos, final BlockPos newPos) {
        final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(serverLevel);
        if (manager == null) return;

        final SubLevel newSubLevel = Sable.HELPER.getContaining(serverLevel, newPos);
        final UUID newSubLevelId = newSubLevel != null ? newSubLevel.getUniqueId() : null;

        for (final ServerRopeStrand strand : new ArrayList<>(manager.getAllStrands())) {
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

    // ── Shearing ─────────────────────────────────────────────────────────────

    public ItemInteractionResult shearLastRope(final ServerPlayer player, final BlockPos pos) {
        if (level == null || level.isClientSide) return ItemInteractionResult.FAIL;

        final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        if (manager == null) return ItemInteractionResult.FAIL;

        ServerRopeStrand targetStrand = null;
        for (final ServerRopeStrand strand : manager.getAllStrands()) {
            final RopeAttachment end   = strand.getAttachment(RopeAttachmentPoint.END);
            final RopeAttachment start = strand.getAttachment(RopeAttachmentPoint.START);
            if ((end   != null && pos.equals(end.blockAttachment())) ||
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
        return currentDispatch;
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

        /**
         * Only run the parent tick when ownedServerStrand is non-null.
         * - currentDispatch: strand is null → tick is a no-op (safe).
         * - committedDispatch: strand is set → full lifecycle tick runs.
         */
        @Override
        public void tick() {
            try {
                if (OWNED_SERVER_STRAND_FIELD.get(this) != null) {
                    super.tick();
                }
            } catch (IllegalAccessException ignored) {}
        }

        /**
         * When called on currentDispatch (ownedServerStrand == null), Sable's sendTrackingData
         * would NPE. Borrow the first committed dispatch's strand temporarily so the packet
         * can be built. The packet data may refer to the first rope rather than the exact strand
         * being tracked, which is acceptable — all ropes are also synced by their committed ticks.
         */
        @Override
        public dev.simulated_team.simulated.network.packets.rope.ClientboundRopeDataPacket makeUpdatePacket() {
            try {
                if (OWNED_SERVER_STRAND_FIELD.get(this) != null) {
                    return super.makeUpdatePacket(); // committed dispatch — normal path
                }
                // currentDispatch: borrow a strand from committed dispatches
                for (final DispatchHolder committed : committedDispatches) {
                    final Object strand = OWNED_SERVER_STRAND_FIELD.get(committed);
                    if (strand != null) {
                        OWNED_SERVER_STRAND_FIELD.set(this, strand);
                        try {
                            return super.makeUpdatePacket();
                        } finally {
                            OWNED_SERVER_STRAND_FIELD.set(this, null);
                        }
                    }
                }
            } catch (IllegalAccessException ignored) {}
            // No strands at all — fall back to parent (will NPE, but this state shouldn't occur
            // because sendTrackingData only queries us when a strand exists at our position).
            return super.makeUpdatePacket();
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
