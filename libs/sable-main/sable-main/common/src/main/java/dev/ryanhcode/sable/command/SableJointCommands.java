package dev.ryanhcode.sable.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.rotary.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class SableJointCommands {

    public static final SimpleCommandExceptionType MISSING_JOINT_SUBLEVEL_TARGET =
            new SimpleCommandExceptionType(Component.translatable("commands.sable.joint.missing_sublevel_target"));

    /**
     * Adds the following commands:
     * <ul>
     *     <li>{@code /sable joint add <subLevel> <subLevel> radial <pos1> <pos2> <axis1> <axis2>}</li>
     * </ul>
     */
    public static void register(final LiteralArgumentBuilder<CommandSourceStack> sableBuilder, final CommandBuildContext buildContext) {

        sableBuilder.then(Commands.literal("joint")
                .then(Commands.literal("add")
                        .then(Commands.argument("subLevel1", SubLevelArgumentType.subLevels())
                                .then(Commands.argument("subLevel2", SubLevelArgumentType.subLevels())
                                        .then(Commands.literal("rotary")
                                                .then(Commands.argument("pos1", Vec3Argument.vec3(false))
                                                        .then(Commands.argument("pos2", Vec3Argument.vec3(false))
                                                                .then(Commands.argument("axis1", Vec3Argument.vec3(false))
                                                                        .then(Commands.argument("axis2", Vec3Argument.vec3(false))
                                                                                .executes(SableJointCommands::executeAddJointCommand)))))))))
        );

    }

    private static int executeAddJointCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerSubLevelContainer container = SableCommandHelper.requireSubLevelContainer(ctx);
        final PhysicsPipeline pipeline = SableCommandHelper.requireSubLevelPhysicsSystem(container).getPipeline();
        addRotaryJoint(
                pipeline,
                SubLevelArgumentType.getSubLevels(ctx, "subLevel1"),
                SubLevelArgumentType.getSubLevels(ctx, "subLevel2"),
                Vec3Argument.getVec3(ctx, "pos1"), Vec3Argument.getVec3(ctx, "pos2"),
                Vec3Argument.getVec3(ctx, "axis1"), Vec3Argument.getVec3(ctx, "axis2")
        );

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.sable.joint.success"), true);
        return 0;
    }

    private static void addRotaryJoint(
            final PhysicsPipeline pipeline,
            final Collection<ServerSubLevel> subLevel1,
            final Collection<ServerSubLevel> subLevel2,
            final Vec3 pos1, final Vec3 pos2,
            final Vec3 axis1, final Vec3 axis2
    ) throws CommandSyntaxException {
        final RotaryConstraintConfiguration constraintConfig = new RotaryConstraintConfiguration(
                JOMLConversion.toJOML(pos1),
                JOMLConversion.toJOML(pos2),
                JOMLConversion.toJOML(axis1),
                JOMLConversion.toJOML(axis2)
        );

        final ServerSubLevel jointSubLevel1 = subLevel1.stream().findFirst()
                .orElseThrow(MISSING_JOINT_SUBLEVEL_TARGET::create);
        final ServerSubLevel jointSubLevel2 = subLevel2.stream().findFirst()
                .orElseThrow(MISSING_JOINT_SUBLEVEL_TARGET::create);

        pipeline.addConstraint(jointSubLevel1, jointSubLevel2, constraintConfig);
    }

}
