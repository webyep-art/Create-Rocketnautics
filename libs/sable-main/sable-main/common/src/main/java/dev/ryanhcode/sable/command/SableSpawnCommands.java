package dev.ryanhcode.sable.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.rotary.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.SchematicLoader;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.Collection;
import java.util.Locale;

public class SableSpawnCommands {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (commandContext, suggestionsBuilder) -> {
        final MinecraftServer server = commandContext.getSource().getServer();
        return SchematicLoader.getSchematics(server).thenCompose(schematics -> {
            final String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
            SharedSuggestionProvider.filterResources(schematics, remaining, resourceLocation -> resourceLocation, resourceLocation -> {
                final String path = resourceLocation.getPath();
                suggestionsBuilder.suggest(path.substring("schematics/".length(), path.length() - ".nbt".length()));
            });
            return suggestionsBuilder.buildFuture();
        });
    };

    private static final BlockState DEFAULT_SPAWN_BLOCKSTATE = Blocks.STONE.defaultBlockState();

    /**
     * Adds the following commands:
     * <ul>
     *     <li>{@code /sable spawn jenga <height> [name]}</li>
     *     <li>{@code /sable spawn <grid|platform|sphere> <size> [block] [name]}</li>
     *     <li>{@code /sable spawn block <block> [name]}</li>
     *     <li>{@code /sable spawn clone <selector> [name]}</li>
     *     <li>{@code /sable spawn schematic <schematic>}</li>
     *     <li>{@code /sable spawn <joint_test|rope_test>}</li>
     *     <li>{@code /sable spawn <slope_test> [name]}</li>
     * </ul>
     */
    public static void register(final LiteralArgumentBuilder<CommandSourceStack> sableBuilder, final CommandBuildContext buildContext) {

        sableBuilder.then(Commands.literal("spawn")
                .then(Commands.literal("jenga")
                        .then(namedSpawnFinale(Commands.argument("height", IntegerArgumentType.integer(1, 256)), SableSpawnCommands::spawnJenga)))

                .then(Commands.literal("clone")
                        .then(namedSpawnFinale(Commands.argument("sub_level", SubLevelArgumentType.singleSubLevel()), SableSpawnCommands::cloneSubLevel)))

                .then(Commands.literal("sphere")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(2, 200))
                                .executes((ctx) -> SableSpawnCommands.spawnSphere(ctx, DEFAULT_SPAWN_BLOCKSTATE, null))
                                .then(namedSpawnFinale(Commands.argument("block", BlockStateArgument.block(buildContext)),
                                        (ctx, name) -> SableSpawnCommands.spawnSphere(ctx, BlockStateArgument.getBlock(ctx, "block").getState(), name)))))

                .then(Commands.literal("schematic")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(SUGGEST_TEMPLATES)
                                .executes(SableSpawnCommands::executeSpawnSchematicCommand)))

                .then(Commands.literal("joint_test")
                        .executes(SableSpawnCommands::executeSpawnJointTestCommand))

                .then(namedSpawnFinale(Commands.literal("slope_test"), SableSpawnCommands::spawnSlopeTest))

                .then(Commands.literal("rope_test")
                        .executes(SableSpawnCommands::executeSpawnRopeTestCommand))

                .then(Commands.literal("grid")
                        .then(Commands.argument("sideLength", IntegerArgumentType.integer(1, 32))
                                .executes((ctx) -> SableSpawnCommands.spawnGrid(ctx, DEFAULT_SPAWN_BLOCKSTATE, null))
                                .then(namedSpawnFinale(Commands.argument("block", BlockStateArgument.block(buildContext)),
                                        (ctx, name) -> SableSpawnCommands.spawnGrid(ctx, BlockStateArgument.getBlock(ctx, "block").getState(), name)))))

                .then(Commands.literal("block")
                        .executes((ctx) -> spawnBlock(ctx, DEFAULT_SPAWN_BLOCKSTATE, null))
                        .then(namedSpawnFinale(Commands.argument("block", BlockStateArgument.block(buildContext)),
                                (ctx, name) -> spawnBlock(ctx, BlockStateArgument.getBlock(ctx, "block").getState(), name))))

                .then(Commands.literal("platform")
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 32))
                                .executes((ctx) -> SableSpawnCommands.spawnPlatform(ctx, DEFAULT_SPAWN_BLOCKSTATE, null))
                                .then(namedSpawnFinale(Commands.argument("block", BlockStateArgument.block(buildContext)),
                                        (ctx, name) -> SableSpawnCommands.spawnPlatform(ctx, BlockStateArgument.getBlock(ctx, "block").getState(), name)))))
        );

    }

    @FunctionalInterface
    private interface NamedSpawnInvoker<S> {
        int run(CommandContext<S> ctx, @Nullable String name) throws CommandSyntaxException;
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T namedSpawnFinale(final T builder, final NamedSpawnInvoker<CommandSourceStack> invoker) {
        builder.executes((ctx) -> invoker.run(ctx, null));
        builder.then(Commands.argument("name", StringArgumentType.string())
                        .executes((ctx) -> invoker.run(ctx, StringArgumentType.getString(ctx, "name"))));
        return builder;
    }

    private static int spawnJenga(final CommandContext<CommandSourceStack> ctx, @Nullable final String name) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final SubLevelContainer container = SableCommandHelper.requireSubLevelContainer(ctx);
        final Vec3 pos = Vec3.atCenterOf(BlockPos.containing(source.getPosition()));
        final int height = IntegerArgumentType.getInteger(ctx, "height");

        for (int yOffset = 0; yOffset < height; yOffset++) {
            final Direction.Axis axis = yOffset % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z;
            final Direction.Axis perpendicular = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;

            for (int index = -1; index <= 1; index++) {
                final Pose3d pose = new Pose3d();
                final Vector3d position = pose.position();
                position.set(pos.x, pos.y, pos.z);

                if (index != 0) {
                    position.add(JOMLConversion.atLowerCornerOf(Direction.get(index == 1 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE, axis).getNormal()));
                }
                position.add(0.0, yOffset, 0.0);
                final Vector3d positionBackup = new Vector3d(position);

                final SubLevel subLevel = container.allocateNewSubLevel(pose);
                subLevel.setName(name);
                final LevelPlot plot = subLevel.getPlot();

                final ChunkPos center = plot.getCenterChunk();
                plot.newEmptyChunk(center);


                final EmbeddedPlotLevelAccessor accessor = plot.getEmbeddedLevelAccessor();
                accessor.setBlock(BlockPos.ZERO, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
                for (int block = -1; block <= 1; block++) {
                    final BlockPos blockPos = BlockPos.ZERO.relative(Direction.get(Direction.AxisDirection.POSITIVE, perpendicular), block);

                    BlockState state = Blocks.OAK_PLANKS.defaultBlockState();

                    if (index == 0) {
                        state = Blocks.SPRUCE_PLANKS.defaultBlockState();
                    }

                    accessor.setBlock(blockPos, state, 3);
                }
                subLevel.logicalPose().position().set(positionBackup);
                subLevel.updateLastPose();
            }
        }

        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "jenga"), false);
        return 1;
    }

    private static int cloneSubLevel(final CommandContext<CommandSourceStack> ctx, @Nullable final String name) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerSubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);
        final ServerSubLevel toClone = SubLevelArgumentType.getSingleSubLevel(ctx, "sub_level");

        final BoundingBox3dc worldBounds = toClone.boundingBox();
        final double height = worldBounds.maxY() - worldBounds.minY();

        final CompoundTag tag = toClone.getPlot().save();

        final ServerSubLevel subLevel = (ServerSubLevel) plotContainer.allocateNewSubLevel(
                new Pose3d(
                        toClone.logicalPose().position().add(0, height * 1.2 + 2, 0, new Vector3d()),
                        new Quaterniond(), new Vector3d(0), new Vector3d(1)
                )
        );
        final ServerLevelPlot plot = subLevel.getPlot();
        plot.load(tag);
        subLevel.updateLastPose();
        if (name != null) {
            subLevel.setName(name);
        }

        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.clone.success"), false);
        return 1;
    }

    private static int spawnSphere(final CommandContext<CommandSourceStack> ctx, final BlockState material, @Nullable final String name) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();

        final SubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);

        Vec3 playerPos = source.getPosition();
        playerPos = Vec3.atCenterOf(BlockPos.containing(playerPos));

        final Pose3d pose = new Pose3d();
        pose.position().set(playerPos.x, playerPos.y, playerPos.z);

        final SubLevel subLevel = plotContainer.allocateNewSubLevel(pose);
        subLevel.setName(name);

        final LevelPlot plot = subLevel.getPlot();

        final ChunkPos center = plot.getCenterChunk();

        final int radius = IntegerArgumentType.getInteger(ctx, "radius");
        final int radiusChunks = (radius + 8) / 16;
        for (int x = -radiusChunks; x <= radiusChunks; x++) {
            for (int z = -radiusChunks; z <= radiusChunks; z++) {
                plot.newEmptyChunk(new ChunkPos(center.x + x, center.z + z));
            }
        }

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    pos.set(x, y, z);
                    if (pos.distSqr(BlockPos.ZERO) <= radius * radius) {
                        plot.getEmbeddedLevelAccessor().setBlock(pos, material, 3);
                    }
                }
            }
        }
        subLevel.updateLastPose();

        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "sphere"), false);
        return 1;
    }

    private static int executeSpawnSchematicCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();

        final StructureTemplate template = SchematicLoader.loadSchematic(level, ResourceLocation.fromNamespaceAndPath("sable", StringArgumentType.getString(ctx, "name")));

        if (template == null) {
            source.sendFailure(Component.translatable("commands.sable.place_schematic.failure"));
            return 0;
        }

        final SubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);

        final Vec3 spawnPos = source.getPosition();

        final Pose3d pose = new Pose3d();
        pose.position().set(spawnPos.x, spawnPos.y, spawnPos.z);

        final SubLevel sublevel = plotContainer.allocateNewSubLevel(pose);
        final LevelPlot plot = sublevel.getPlot();

        final ChunkPos center = plot.getCenterChunk();

        final BoundingBox bounds = template.getBoundingBox(BlockPos.ZERO, Rotation.NONE, BlockPos.ZERO, Mirror.NONE);

        final int minChunkX = bounds.minX() >> 4;
        final int minChunkZ = bounds.minZ() >> 4;

        final int maxChunkX = bounds.maxX() >> 4;
        final int maxChunkZ = bounds.maxZ() >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                plot.newEmptyChunk(new ChunkPos(center.x + x, center.z + z));
            }
        }

        final EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
        template.placeInWorld(embedded, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), RandomSource.create(), 3);
        sublevel.updateLastPose();
        sublevel.logicalPose().position().set(spawnPos.x, spawnPos.y, spawnPos.z);

        source.sendSuccess(() -> Component.translatable("commands.sable.place_schematic.success"), false);
        return 1;
    }

    private static int executeSpawnRopeTestCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();

        final ServerSubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);
        final SubLevelPhysicsSystem system = SableCommandHelper.requireSubLevelPhysicsSystem(plotContainer);

        final Vec3 playerPos = Vec3.atCenterOf(BlockPos.containing(source.getPosition()));
        final Collection<Vector3d> points = new ObjectArrayList<>();

        for (int i = 0; i < 10; i++) {
            points.add(JOMLConversion.toJOML(playerPos).add(i, 0, 0));
        }

        final RopePhysicsObject object = new RopePhysicsObject(points, 0.25);
        system.addObject(object);
        object.setAttachment(RopeHandle.AttachmentPoint.START, JOMLConversion.toJOML(playerPos), null);

        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "rope_test"), false);
        return 1;
    }


    private static int executeSpawnJointTestCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();

        final ServerSubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);

        final Vec3 playerPos = Vec3.atCenterOf(BlockPos.containing(source.getPosition()));

        final Pose3d pose1 = new Pose3d();
        pose1.position().set(playerPos.x, playerPos.y, playerPos.z);

        final Pose3d pose2 = new Pose3d();
        pose2.position().set(playerPos.x, playerPos.y + 1.0, playerPos.z);

        final ServerSubLevel subLevelA = (ServerSubLevel) plotContainer.allocateNewSubLevel(pose1);
        final ServerSubLevel subLevelB = (ServerSubLevel) plotContainer.allocateNewSubLevel(pose2);

        final LevelPlot plotA = subLevelA.getPlot();
        final LevelPlot plotB = subLevelB.getPlot();

        plotA.newEmptyChunk(plotA.getCenterChunk());
        plotA.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, Blocks.STONE.defaultBlockState(), 3);

        plotB.newEmptyChunk(plotB.getCenterChunk());
        plotB.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, Blocks.STONE.defaultBlockState(), 3);

        final RotaryConstraintConfiguration config = new RotaryConstraintConfiguration(
                JOMLConversion.atBottomCenterOf(plotA.getCenterBlock().above().above()),
                JOMLConversion.atBottomCenterOf(plotB.getCenterBlock()),
                JOMLConversion.atLowerCornerOf(Direction.UP.getNormal()),
                JOMLConversion.atLowerCornerOf(Direction.UP.getNormal())
        );
//        final FreeConstraintConfiguration config = new FreeConstraintConfiguration();

        final PhysicsConstraintHandle handle = SableCommandHelper.requireSubLevelPhysicsSystem(plotContainer)
                .getPipeline().addConstraint(subLevelA, subLevelB, config);

        handle.setContactsEnabled(false);
        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "joint_test"), false);
        return 1;
    }

    private static int spawnSlopeTest(final CommandContext<CommandSourceStack> ctx, final @Nullable String name) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();

        final ServerSubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);

        final Vec3 playerPos = Vec3.atCenterOf(BlockPos.containing(source.getPosition()));

        final int gridSize = 9;
        final double yawRange = Math.toRadians(90.0);
        final double pitchRange = Math.toRadians(90.00);
        final int rad = 3;

        final int spacing = rad * 2 + 2;
        for (int xo = 0; xo <= gridSize; xo++) {
                for (int zo = 0; zo <= gridSize; zo++) {

                    final Pose3d pose1 = new Pose3d();
                    pose1.position().set(playerPos.x, playerPos.y, playerPos.z);

                    final ServerSubLevel subLevel = (ServerSubLevel) plotContainer.allocateNewSubLevel(pose1);
                    subLevel.setName(name);

                    final LevelPlot plotA = subLevel.getPlot();

                    final BlockState block = Blocks.END_STONE.defaultBlockState();
                    plotA.newEmptyChunk(plotA.getCenterChunk());

                    for (int lx = -rad; lx < rad; lx ++)  {
                        for (int lz = -rad; lz < rad; lz++) {
                            plotA.getEmbeddedLevelAccessor().setBlock(new BlockPos(lx, 0, lz), block, 3);
                        }
                    }

                    final Vector3d pos = new Vector3d(playerPos.x + xo * spacing, playerPos.y, playerPos.z + zo * spacing);
                    final Quaterniond orientation = new Quaterniond();

                    orientation.rotateY(xo * yawRange / gridSize);
                    orientation.rotateX(zo * pitchRange / gridSize);

                    SableCommandHelper.requireSubLevelPhysicsPipeline(ctx).teleport(subLevel, pos, orientation);
                }
        }
        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "slope_test"), false);
        return 1;
    }

    private static int spawnGrid(final CommandContext<CommandSourceStack> ctx, final BlockState material, final @Nullable String name) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();

        final SubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);

        final Vec3 playerPos = source.getPosition();

        final int sideLength = IntegerArgumentType.getInteger(ctx, "sideLength");

        final Vec3[] positions = new Vec3[sideLength * sideLength * sideLength];

        for (int x = 0; x < sideLength; x++) {
            for (int z = 0; z < sideLength; z++) {
                for (int y = 0; y < sideLength; y++) {
                    positions[x * sideLength * sideLength + z * sideLength + y] = new Vec3(x, y, z).scale(2.1).add(playerPos);
                }
            }
        }

        for (final Vec3 subLevelPos : positions) {
            final Pose3d pose = new Pose3d();
            pose.position().set(subLevelPos.x, subLevelPos.y, subLevelPos.z);

            final SubLevel subLevel = plotContainer.allocateNewSubLevel(pose);
            subLevel.setName(name);
            final LevelPlot plot = subLevel.getPlot();

            final ChunkPos center = plot.getCenterChunk();
            plot.newEmptyChunk(center);

            plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, material, 3);
            subLevel.updateLastPose();
        }

        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "grid"), false);
        return 1;
    }

    private static int spawnBlock(final CommandContext<CommandSourceStack> ctx, final BlockState material, final @Nullable String name) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();

        final SubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);

        final Vec3 playerPos = source.getPosition();

        final Pose3d pose = new Pose3d();
        pose.position().set(playerPos.x, playerPos.y, playerPos.z);

        final SubLevel subLevel = plotContainer.allocateNewSubLevel(pose);
        subLevel.setName(name);
        final LevelPlot plot = subLevel.getPlot();

        final ChunkPos center = plot.getCenterChunk();
        plot.newEmptyChunk(center);

        plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, material, 3);
        subLevel.updateLastPose();

        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "block"), false);
        return 1;
    }

    private static int spawnPlatform(final CommandContext<CommandSourceStack> ctx, final BlockState material, final @Nullable String name) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();

        final SubLevelContainer plotContainer = SableCommandHelper.requireSubLevelContainer(ctx);

        final Vec3 playerPos = source.getPosition();

        final Pose3d pose = new Pose3d();
        pose.position().set(playerPos.x, playerPos.y, playerPos.z);

        final SubLevel subLevel = plotContainer.allocateNewSubLevel(pose);
        subLevel.setName(name);
        final LevelPlot plot = subLevel.getPlot();

        final ChunkPos center = plot.getCenterChunk();

        final int size = IntegerArgumentType.getInteger(ctx, "size");
        final int radiusChunks = (size + 8) / 16;
        for (int x = -radiusChunks; x <= radiusChunks; x++) {
            for (int z = -radiusChunks; z <= radiusChunks; z++) {
                plot.newEmptyChunk(new ChunkPos(center.x + x, center.z + z));
            }
        }
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                plot.getEmbeddedLevelAccessor().setBlock(new BlockPos(x, 0, z), material, 2);
            }
        }
        subLevel.updateLastPose();
        SableCommandHelper.requireSubLevelPhysicsPipeline(ctx).teleport(
                (ServerSubLevel) subLevel,
                new Vector3d(playerPos.x, playerPos.y, playerPos.z),
                pose.orientation()
        );

        source.sendSuccess(() -> Component.translatable("commands.sable.spawn.success", "platform"), false);
        return 1;
    }

}
