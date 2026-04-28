package dev.ryanhcode.sable.physics.floating_block;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FloatingClusterContainer {
    public List<FloatingBlockCluster> clusters = new ArrayList<>();
    private final Long2ObjectMap<BlockState> addedBlocks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<BlockState> removedBlocks = new Long2ObjectOpenHashMap<>();

    public final Vector3d positionOffset = new Vector3d();
    public final Quaterniond rotationOffset = new Quaterniond();
    public final Vector3d velocity = new Vector3d();
    public final Vector3d angularVelocity = new Vector3d();

    public boolean needsTicking()
    {
        return !this.addedBlocks.isEmpty() || !this.clusters.isEmpty();
    }

    public void processBlockChanges(Vector3dc centerOfMass) {

        for (final Map.Entry<Long, BlockState> entry : this.removedBlocks.entrySet()) {
            final BlockPos blockPos = BlockPos.of(entry.getKey());
            final Vector3d pos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5).sub(centerOfMass);
            this.removeFloatingBlock(entry.getValue(), pos);
        }

        for (final Map.Entry<Long, BlockState> entry : this.addedBlocks.entrySet()) {
            final BlockPos blockPos = BlockPos.of(entry.getKey());
            final Vector3d pos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5).sub(centerOfMass);
            this.addFloatingBlock(entry.getValue(), pos);
        }


        this.addedBlocks.clear();
        this.removedBlocks.clear();
    }

    public void addFloatingBlock(final BlockState state, final Vector3d pos) {
        final FloatingBlockMaterial material = PhysicsBlockPropertyHelper.getFloatingMaterial(state);
        assert material != null : "Floating Material desync on adding";

        FloatingBlockCluster foundCluster = null;
        for (final FloatingBlockCluster cluster : this.clusters) {
            if (cluster.getMaterial().equals(material)) {
                foundCluster = cluster;
                break;
            }
        }
        if (foundCluster == null) {
            foundCluster = new FloatingBlockCluster(material);
            this.clusters.add(foundCluster);
        }
        final double scale = PhysicsBlockPropertyHelper.getFloatingScale(state);
        foundCluster.getBlockData().addFloatingBlock(pos, scale);

    }

    public void removeFloatingBlock(final BlockState state, final Vector3d pos) {
        final FloatingBlockMaterial material = PhysicsBlockPropertyHelper.getFloatingMaterial(state);

        assert material != null : "Floating Material desync on removing";

        FloatingBlockCluster foundCluster = null;
        for (final FloatingBlockCluster cluster : this.clusters) {
            if (cluster.getMaterial().equals(material)) {
                foundCluster = cluster;
                break;
            }
        }

        if (foundCluster != null) {
            final double scale = PhysicsBlockPropertyHelper.getFloatingScale(state);
            foundCluster.getBlockData().removeFloatingBlock(pos, scale);
            if (foundCluster.getBlockData().blockCount == 0)
                this.clusters.remove(foundCluster);
        }
    }

    public void queueAddFloatingBlock(final BlockState state, final BlockPos pos) {
        final long longKey = pos.asLong();
        if (PhysicsBlockPropertyHelper.getFloatingMaterial(state) != null) {
            if(!this.removedBlocks.remove(longKey,state))
                this.addedBlocks.put(longKey, state);
        }
    }

    public void queueRemoveFloatingBlock(final BlockState state, final BlockPos pos) {
        final long longKey = pos.asLong();
        if (PhysicsBlockPropertyHelper.getFloatingMaterial(state) != null) {
            if(!this.addedBlocks.remove(longKey,state))
                this.removedBlocks.put(longKey, state);
        }
    }
}
