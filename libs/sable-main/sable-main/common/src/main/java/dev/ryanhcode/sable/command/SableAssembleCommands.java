package dev.ryanhcode.sable.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;
import java.util.stream.IntStream;

public class SableAssembleCommands {

    public static final int DEFAULT_CONNECTED_ASSEMBLY_CAPACITY = 256_000;

    /**
     * Adds the following commands:
     * <ul>
     *     <li>{@code /sable assemble area <from> <to>}</li>
     *     <li>{@code /sable assemble connected [<from>] [<capacity>]}</li>
     *     <li>{@code /sable assemble sphere <radius> [<origin>]}</li>
     *     <li>{@code /sable assemble cube <range> [<origin>]}</li>
     *     <li>{@code /sable shatter sub_level <sub_level>}</li>
     *     <li>{@code /sable shatter connected [<from>] [<capacity>]}</li>
     *     <li>{@code /sable shatter sphere <radius> [<origin>]}</li>
     *     <li>{@code /sable shatter cube <range> [<origin>]}</li>
     *     <li>{@code /sable shatter area <from> <to>}</li>
     * </ul>
     */
    public static void register(final LiteralArgumentBuilder<CommandSourceStack> sableBuilder, final CommandBuildContext buildContext) {
        sableBuilder
                .then(Commands.literal("assemble")
                        .then(Commands.literal("shatter")
                                .then(Commands.literal("sub_level")
                                        .then(Commands.argument("sub_level", SubLevelArgumentType.subLevels())
                                                .executes(SableAssembleCommands::executeShatterSubLevelCommand)))
                                .then(Commands.literal("connected")
                                        .executes((ctx) ->
                                                SableAssembleCommands.executeShatterConnected(ctx, BlockPos.containing(ctx.getSource().getPosition().subtract(0, 1, 0)), DEFAULT_CONNECTED_ASSEMBLY_CAPACITY))
                                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                                .executes((ctx) ->
                                                        SableAssembleCommands.executeShatterConnected(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "from"), DEFAULT_CONNECTED_ASSEMBLY_CAPACITY))
                                                .then(Commands.argument("capacity", IntegerArgumentType.integer(1, DEFAULT_CONNECTED_ASSEMBLY_CAPACITY * 100))
                                                        .executes((ctx) ->
                                                                SableAssembleCommands.executeShatterConnected(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "from"), IntegerArgumentType.getInteger(ctx, "capacity"))))))
                                .then(Commands.literal("sphere")
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 128))
                                                .executes((ctx) -> SableAssembleCommands.executeShatterSphereCommand(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                                                .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                                        .executes((ctx) -> SableAssembleCommands.executeShatterSphereCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin"))))))
                                .then(Commands.literal("cube")
                                        .then(Commands.argument("range", IntegerArgumentType.integer(0, 128))
                                                .executes((ctx) -> SableAssembleCommands.executeShatterCubeCommand(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                                                .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                                        .executes((ctx) -> SableAssembleCommands.executeShatterCubeCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin"))))))
                                .then(Commands.literal("area")
                                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                            .executes(SableAssembleCommands::executeShatterAreaCommand)))))

                        .then(Commands.literal("area")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                            .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                    .executes(SableAssembleCommands::executeAssembleAreaCommand))))

                        .then(Commands.literal("connected")
                                .executes((ctx) ->
                                        SableAssembleCommands.executeAssembleConnectedCommand(ctx, BlockPos.containing(ctx.getSource().getPosition().subtract(0, 1, 0)), DEFAULT_CONNECTED_ASSEMBLY_CAPACITY))
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .executes((ctx) ->
                                                SableAssembleCommands.executeAssembleConnectedCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "from"), DEFAULT_CONNECTED_ASSEMBLY_CAPACITY))
                                        .then(Commands.argument("capacity", IntegerArgumentType.integer(1, DEFAULT_CONNECTED_ASSEMBLY_CAPACITY * 100))
                                                .executes((ctx) ->
                                                        SableAssembleCommands.executeAssembleConnectedCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "from"), IntegerArgumentType.getInteger(ctx, "capacity"))))))

                        .then(Commands.literal("sphere")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 256))
                                        .executes(ctx -> SableAssembleCommands.executeAssembleSphereCommand(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                                        .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                                .executes(ctx -> SableAssembleCommands.executeAssembleSphereCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin"))))))

                        .then(Commands.literal("cube")
                                .then(Commands.argument("range", IntegerArgumentType.integer(0, 256))
                                        .executes(ctx -> SableAssembleCommands.executeAssembleCubeCommand(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                                        .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                                .executes(ctx -> SableAssembleCommands.executeAssembleCubeCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin")))))));
    }

    private static int executeShatterConnected(final CommandContext<CommandSourceStack> ctx, final BlockPos assemblyOrigin, final int assemblyCapacity) throws CommandSyntaxException {
        final ServerLevel level = ctx.getSource().getLevel();

        final SubLevelAssemblyHelper.GatherResult result = SubLevelAssemblyHelper.gatherConnectedBlocks(assemblyOrigin, level, assemblyCapacity, null);
        if (result.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS) {
            ctx.getSource().sendFailure(Component.translatable(switch (result.assemblyState()) {
                case TOO_MANY_BLOCKS -> "commands.sable.sub_level.shatter.connected.too_many_blocks";
                case NO_BLOCKS -> "commands.sable.sub_level.shatter.no_blocks";
                default -> throw new IllegalStateException("Unexpected value: " + result.assemblyState());
            }, result.assemblyState() == SubLevelAssemblyHelper.GatherResult.State.TOO_MANY_BLOCKS ? assemblyCapacity : 0));
            return 0;
        }

        final int blocksShattered = shatterBlocks(result.blocks(), level);
        if (blocksShattered == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.connected.success", blocksShattered), true);
        return blocksShattered;
    }

    private static int executeShatterSubLevelCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerLevel level = ctx.getSource().getLevel();
        final Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx,
                "sub_level");

        if (subLevels.isEmpty()) {
            throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
        }

        final IntStream shatteredAmounts = subLevels
                .stream()
                .filter(subLevel -> { //Filter out single block sub-levels
                    int solidBlockCount = 0;
                    for (final Iterator<BlockPos> it = BlockPos.betweenClosedStream(subLevel.getPlot().getBoundingBox().toMojang()).iterator(); it.hasNext(); ) {
                        final BlockPos pos = it.next();
                        if (VoxelNeighborhoodState.isSolid(level, pos, level.getBlockState(pos))) {
                            solidBlockCount++;
                            if (solidBlockCount > 1) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .map(subLevel -> subLevel.getPlot().getBoundingBox())
                .mapToInt(bounds -> shatterBoundingBox(bounds, level));

        int blocksShattered = 0;
        int sublevelsShattered = 0;

        for (final PrimitiveIterator.OfInt it = shatteredAmounts.iterator(); it.hasNext(); ) {
            final int i = it.next();
            blocksShattered += i;
            sublevelsShattered ++;
        }

        if (sublevelsShattered == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.shatter.sub_level.only_single_block"));
            return 0;
        }

        final int finalSublevelsShattered = sublevelsShattered;
        final int finalBlocksShattered = blocksShattered;
        if (sublevelsShattered == 1) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.sub_level.success", Component.translatable("commands.sable.sub_level"), finalBlocksShattered), true);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.sub_level.success", Component.translatable("commands.sable.sub_levels", finalSublevelsShattered), finalBlocksShattered), true);
        }
        return blocksShattered;
    }

    private static int executeShatterAreaCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerLevel level = ctx.getSource().getLevel();
        final BoundingBox3i boundingBox = new BoundingBox3i(BlockPosArgument.getLoadedBlockPos(ctx, "from"), BlockPosArgument.getLoadedBlockPos(ctx, "to"));

        final int blocksShattered = shatterBoundingBox(boundingBox, level);
        if (blocksShattered == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.region.success", blocksShattered), true);
        return blocksShattered;
    }

    private static int executeShatterSphereCommand(final CommandContext<CommandSourceStack> ctx, final BlockPos origin) {
        final ServerLevel level = ctx.getSource().getLevel();
        final int radius = IntegerArgumentType.getInteger(ctx, "radius");
        final BoundingBox boundingBox = BoundingBox.fromCorners(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius)
        );

        final int radiusSquared = radius * radius;

        final List<BlockPos> blocks = BlockPos.betweenClosedStream(boundingBox).map(BlockPos::immutable).toList();
        final List<BlockPos> blocksInRadius = new ArrayList<>();
        for (final BlockPos blockPos : blocks) {
            if (origin.distSqr(blockPos) > radiusSquared) {
                continue;
            }
            blocksInRadius.add(blockPos);
        }
        final int blocksShattered = shatterBlocks(blocksInRadius, level);
        if (blocksShattered == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.radius.success", blocksShattered), true);
        return blocksShattered;
    }

    private static int executeShatterCubeCommand(final CommandContext<CommandSourceStack> ctx, final BlockPos origin) {
        final ServerLevel level = ctx.getSource().getLevel();
        final int radius = IntegerArgumentType.getInteger(ctx, "range");
        final BoundingBox3i boundingBox = new BoundingBox3i(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius)
        );

        final int blocksShattered = shatterBoundingBox(boundingBox, level);
        if (blocksShattered == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.range.success", blocksShattered), true);
        return blocksShattered;
    }

    private static int shatterBoundingBox(final BoundingBox3ic boundingBox, final ServerLevel level) {
        return shatterBlocks(BlockPos.betweenClosedStream(boundingBox.toMojang()).map(BlockPos::immutable).toList(), level);
    }

    private static int shatterBlocks(final Collection<BlockPos> blocks, final ServerLevel level) {
        //Remove fragile blocks
        for (final BlockPos pos : blocks) {
            if (!VoxelNeighborhoodState.isSolid(level, pos, level.getBlockState(pos))) {
                level.destroyBlock(pos, true);
            }
        }
        int shattered = 0;
        for (final BlockPos anchor : blocks) {
            if (shatterBlockToSubLevel(level, anchor)) {
                shattered++;
            }
        }
        return shattered;
    }

    private static boolean shatterBlockToSubLevel(final ServerLevel level, final BlockPos anchor) {
        if (!VoxelNeighborhoodState.isSolid(level, anchor, level.getBlockState(anchor))) {
            return false;
        }

        final BoundingBox3i bounds = new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX() + 1, anchor.getY() + 1, anchor.getZ() + 1);
        bounds.set(
                bounds.minX - 1,
                bounds.minY - 1,
                bounds.minZ - 1,
                bounds.maxX + 1,
                bounds.maxY + 1,
                bounds.maxZ + 1
        );
        SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor), bounds);
        return true;
    }

    private static int executeAssembleAreaCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerLevel level = ctx.getSource().getLevel();
        final BoundingBox boundingBox = BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(ctx, "from"), BlockPosArgument.getLoadedBlockPos(ctx, "to"));

        final List<BlockPos> blocks = BlockPos.betweenClosedStream(boundingBox).map(BlockPos::immutable).toList();
        final BlockPos anchor = blocks.getFirst();

        final BoundingBox3i bounds = new BoundingBox3i(boundingBox);
        bounds.set(
                bounds.minX - 1,
                bounds.minY - 1,
                bounds.minZ - 1,
                bounds.maxX + 1,
                bounds.maxY + 1,
                bounds.maxZ + 1
        );

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds);
        if (subLevel.getMassTracker().isInvalid()) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.assemble.no_blocks"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.region.success", blocks.size()), true);
        return 1;
    }

    private static int executeAssembleCubeCommand(final CommandContext<CommandSourceStack> ctx, final BlockPos origin) {
        final ServerLevel level = ctx.getSource().getLevel();
        final int range = IntegerArgumentType.getInteger(ctx, "range");
        final BoundingBox boundingBox = BoundingBox.fromCorners(origin.offset(-range, -range, -range), origin.offset(range, range, range));

        final List<BlockPos> blocks = BlockPos.betweenClosedStream(boundingBox).map(BlockPos::immutable).toList();
        final BlockPos anchor = blocks.getFirst();

        final BoundingBox3i bounds = new BoundingBox3i(boundingBox);
        bounds.set(
                bounds.minX - 1,
                bounds.minY - 1,
                bounds.minZ - 1,
                bounds.maxX + 1,
                bounds.maxY + 1,
                bounds.maxZ + 1
        );

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds);
        if (subLevel.getMassTracker().isInvalid()) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.assemble.no_blocks"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.range.success", blocks.size()), true);
        return 1;
    }

    private static int executeAssembleConnectedCommand(final CommandContext<CommandSourceStack> ctx, final BlockPos assemblyOrigin, final int assemblyCapacity) throws CommandSyntaxException {
        final ServerLevel level = ctx.getSource().getLevel();

        final SubLevelAssemblyHelper.GatherResult result = SubLevelAssemblyHelper.gatherConnectedBlocks(assemblyOrigin, level, assemblyCapacity, null);
        if (result.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS) {
            ctx.getSource().sendFailure(Component.translatable(result.assemblyState().errorKey, result.assemblyState() == SubLevelAssemblyHelper.GatherResult.State.TOO_MANY_BLOCKS ? assemblyCapacity : 0));
            return 0;
        }

        SubLevelAssemblyHelper.assembleBlocks(level, assemblyOrigin, result.blocks(), result.boundingBox());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.connected.success", result.blocks().size()), true);
        return 1;
    }

    private static int executeAssembleSphereCommand(final CommandContext<CommandSourceStack> ctx, final BlockPos origin) {
        final int radius = IntegerArgumentType.getInteger(ctx, "radius");

        final ServerLevel level = ctx.getSource().getLevel();

        final Set<BlockPos> blocks = new HashSet<>();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        final int radiusSquared = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }
                    final BlockPos pos = origin.offset(x, y, z);

                    if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
                        blocks.add(pos);

                        minX = Math.min(minX, pos.getX());
                        minY = Math.min(minY, pos.getY());
                        minZ = Math.min(minZ, pos.getZ());

                        maxX = Math.max(maxX, pos.getX());
                        maxY = Math.max(maxY, pos.getY());
                        maxZ = Math.max(maxZ, pos.getZ());
                    }
                }
            }
        }

        if (blocks.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.sable.sub_level.assemble.no_blocks"));
            return 0;
        }

        final BoundingBox3i bounds = new BoundingBox3i(
                minX, minY, minZ,
                maxX, maxY, maxZ
        );

        SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, bounds);

        final int finalBlocksCount = blocks.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.radius.success", finalBlocksCount), true);
        return 1;
    }

}
