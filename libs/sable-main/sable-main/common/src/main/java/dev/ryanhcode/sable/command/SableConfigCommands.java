package dev.ryanhcode.sable.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.PhysicsConfigData;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SableConfigCommands {

    /**
     * Adds the following commands:
     * <ul>
     *     <li>{@code /sable config <property> <value>}</li>
     * </ul>
     */
    public static void register(final LiteralArgumentBuilder<CommandSourceStack> sableBuilder, final CommandBuildContext buildContext) {

        sableBuilder.then(Commands.literal("config")
                .then(Commands.literal("min_island_size")
                        .then(Commands.argument("size", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> {
                                    final SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(ctx.getSource().getLevel()).physicsSystem();
                                    final PhysicsConfigData config = physicsSystem.getConfig();
                                    config.minDynamicBodiesPerIsland = IntegerArgumentType.getInteger(ctx, "size");
                                    physicsSystem.onConfigUpdated();
                                    return 0;
                                }))
                )
                .then(Commands.literal("contact_spring_natural_frequency")
                        .then(Commands.argument("natural_frequency", FloatArgumentType.floatArg(0.0f))
                                .executes(ctx -> {
                                    final SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(ctx.getSource().getLevel()).physicsSystem();
                                    final PhysicsConfigData config = physicsSystem.getConfig();
                                    config.contactSpringFrequency = FloatArgumentType.getFloat(ctx, "natural_frequency");
                                    physicsSystem.onConfigUpdated();
                                    return 0;
                                }))
                )
                .then(Commands.literal("contact_spring_damping_ratio")
                        .then(Commands.argument("damping_ratio", FloatArgumentType.floatArg(0.0f))
                                .executes(ctx -> {
                                    final SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(ctx.getSource().getLevel()).physicsSystem();
                                    final PhysicsConfigData config = physicsSystem.getConfig();
                                    config.contactSpringDampingRatio = FloatArgumentType.getFloat(ctx, "damping_ratio");
                                    physicsSystem.onConfigUpdated();
                                    return 0;
                                }))
                )
                .then(Commands.literal("solver_iterations")
                        .then(Commands.argument("iterations", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> {
                                    final SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(ctx.getSource().getLevel()).physicsSystem();
                                    final PhysicsConfigData config = physicsSystem.getConfig();
                                    config.solverIterations = IntegerArgumentType.getInteger(ctx, "iterations");
                                    physicsSystem.onConfigUpdated();
                                    return 0;
                                }))
                )
                .then(Commands.literal("stabilization_iterations")
                        .then(Commands.argument("iterations", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> {
                                    final SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(ctx.getSource().getLevel()).physicsSystem();
                                    final PhysicsConfigData config = physicsSystem.getConfig();
                                    config.stabilizationIterations = IntegerArgumentType.getInteger(ctx, "iterations");
                                    physicsSystem.onConfigUpdated();
                                    return 0;
                                }))
                )
                .then(Commands.literal("pgs_iterations")
                        .then(Commands.argument("iterations", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> {
                                    final SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(ctx.getSource().getLevel()).physicsSystem();
                                    final PhysicsConfigData config = physicsSystem.getConfig();
                                    config.pgsIterations = IntegerArgumentType.getInteger(ctx, "iterations");
                                    physicsSystem.onConfigUpdated();
                                    return 0;
                                }))
                )
                .then(Commands.literal("substeps")
                        .then(Commands.argument("substeps", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> {
                                    final SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(ctx.getSource().getLevel()).physicsSystem();
                                    final PhysicsConfigData config = physicsSystem.getConfig();
                                    config.substepsPerTick = IntegerArgumentType.getInteger(ctx, "substeps");
                                    physicsSystem.onConfigUpdated();
                                    return 0;
                                }))
                )
        );

    }
}
