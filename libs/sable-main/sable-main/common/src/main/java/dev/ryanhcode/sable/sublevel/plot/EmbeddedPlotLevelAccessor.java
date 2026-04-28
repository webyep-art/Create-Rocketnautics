package dev.ryanhcode.sable.sublevel.plot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link CommonLevelAccessor} utility for a {@link LevelPlot} that places (0, 0, 0) at the plot center
 */
public class EmbeddedPlotLevelAccessor implements CommonLevelAccessor, ServerLevelAccessor {

    /**
     * The plot that this level accessor is embedded in
     */
    private final LevelPlot plot;

    /**
     * The center of the plot
     */
    private final BlockPos center;

    /**
     * The center chunk of the plot
     */
    private final ChunkPos centerChunk;

    /**
     * The level of the plot
     */
    private final Level level;

    /**
     * Creates a new embedded plot level accessor
     *
     * @param plot The plot to embed
     */
    public EmbeddedPlotLevelAccessor(final LevelPlot plot) {
        this.plot = plot;
        this.level = plot.getSubLevel().getLevel();
        this.center = plot.getCenterBlock();
        this.centerChunk = plot.getCenterChunk();
    }

    @Override
    public float getShade(final Direction direction, final boolean bl) {
        return this.level.getShade(direction, bl);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(final BlockPos blockPos) {
        return this.level.getBlockEntity(blockPos.offset(this.center));
    }

    @Override
    public BlockState getBlockState(final BlockPos blockPos) {
        return this.level.getBlockState(blockPos.offset(this.center));
    }

    @Override
    public FluidState getFluidState(final BlockPos blockPos) {
        return this.level.getFluidState(blockPos.offset(this.center));
    }

    @Override
    public List<Entity> getEntities(@Nullable final Entity entity, final AABB aABB, final Predicate<? super Entity> predicate) {
        return this.level.getEntities(entity, aABB.move(this.center.getX(), this.center.getY(), this.center.getZ()), predicate);
    }

    @Override
    public <T extends Entity> List<T> getEntities(final EntityTypeTest<Entity, T> entityTypeTest, final AABB aABB, final Predicate<? super T> predicate) {
        return this.level.getEntities(entityTypeTest, aABB.move(this.center.getX(), this.center.getY(), this.center.getZ()), predicate);
    }

    @Override
    public List<? extends Player> players() {
        return this.level.players();
    }

    @Override
    public @Nullable ChunkAccess getChunk(final int i, final int j, final ChunkStatus chunkStatus, final boolean bl) {
        return this.level.getChunk(i + this.centerChunk.x, j + this.centerChunk.z, chunkStatus, bl);
    }

    @Override
    public long nextSubTickCount() {
        return this.level.nextSubTickCount();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.level.getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.level.getFluidTicks();
    }

    @Override
    public LevelData getLevelData() {
        return this.level.getLevelData();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(final BlockPos blockPos) {
        return this.level.getCurrentDifficultyAt(blockPos.offset(this.center));
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        return this.level.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.level.getChunkSource();
    }

    @Override
    public boolean hasChunk(final int i, final int j) {
        return this.level.hasChunk(i + this.centerChunk.x, j + this.centerChunk.z);
    }

    @Override
    public RandomSource getRandom() {
        return this.level.getRandom();
    }

    @Override
    public void playSound(@Nullable final Player player, final BlockPos blockPos, final SoundEvent soundEvent, final SoundSource soundSource, final float f, final float g) {
        this.level.playSound(player, blockPos.offset(this.center), soundEvent, soundSource, f, g);
    }

    @Override
    public void addParticle(final ParticleOptions particleOptions, final double d, final double e, final double f, final double g, final double h, final double i) {
        this.level.addParticle(particleOptions, d + this.center.getX(), e + this.center.getY(), f + this.center.getZ(), g, h, i);
    }

    @Override
    public void levelEvent(@Nullable final Player player, final int i, final BlockPos blockPos, final int j) {
        this.level.levelEvent(player, i, blockPos.offset(this.center), j);
    }

    @Override
    public void gameEvent(final Holder<GameEvent> holder, final Vec3 vec3, final GameEvent.Context context) {
        this.level.gameEvent(holder, vec3, context);
    }

    @Override
    public int getHeight(final Heightmap.Types types, final int i, final int j) {
        return this.level.getHeight(types, i + this.center.getX(), j + this.center.getZ());
    }

    @Override
    public int getSkyDarken() {
        return this.level.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.level.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(final int i, final int j, final int k) {
        return this.level.getUncachedNoiseBiome(i + this.center.getX(), j + this.center.getY(), k + this.center.getZ());
    }

    @Override
    public boolean isClientSide() {
        return this.level.isClientSide();
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return this.level.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.level.enabledFeatures();
    }

    @Override
    public boolean isStateAtPosition(final BlockPos blockPos, final Predicate<BlockState> predicate) {
        return this.level.isStateAtPosition(blockPos.offset(this.center), predicate);
    }

    @Override
    public boolean isFluidAtPosition(final BlockPos blockPos, final Predicate<FluidState> predicate) {
        return this.level.isFluidAtPosition(blockPos.offset(this.center), predicate);
    }

    @Override
    public boolean setBlock(final BlockPos blockPos, final BlockState blockState, final int i, final int j) {
        return this.level.setBlock(blockPos.offset(this.center), blockState, i, j);
    }

    @Override
    public boolean removeBlock(final BlockPos blockPos, final boolean bl) {
        return this.level.removeBlock(blockPos.offset(this.center), bl);
    }

    @Override
    public boolean destroyBlock(final BlockPos blockPos, final boolean bl, @Nullable final Entity entity, final int i) {
        return this.level.destroyBlock(blockPos.offset(this.center), bl, entity, i);
    }

    @Override
    public ServerLevel getLevel() {
        return (ServerLevel) this.level;
    }
}
