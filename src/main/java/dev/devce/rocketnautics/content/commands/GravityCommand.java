package dev.devce.rocketnautics.content.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.Set;
import dev.devce.rocketnautics.RocketNautics;

public class GravityCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rn")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("gravity")
                .then(Commands.argument("value", FloatArgumentType.floatArg(-10.0f, 10.0f))
                    .executes(context -> {
                        float value = FloatArgumentType.getFloat(context, "value");
                        GlobalSpacePhysicsHandler.setGravityOverride(value);
                        context.getSource().sendSuccess(() -> Component.literal("Effective gravity set to: " + value), true);
                        return 1;
                    })
                )
                .then(Commands.literal("reset")
                    .executes(context -> {
                        GlobalSpacePhysicsHandler.resetGravityOverride();
                        context.getSource().sendSuccess(() -> Component.literal("Gravity set to automatic calculation"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("calibrate")
                    .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.0, 2.0))
                        .executes(context -> {
                            double multiplier = DoubleArgumentType.getDouble(context, "multiplier");
                            GlobalSpacePhysicsHandler.setCalibration(multiplier);
                            context.getSource().sendSuccess(() -> Component.literal("Physics calibration multiplier set to: " + multiplier), true);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("space")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ResourceKey<Level> spaceKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "space"));
                    ServerLevel spaceLevel = source.getServer().getLevel(spaceKey);
                    if (spaceLevel != null) {
                        context.getSource().getPlayerOrException().teleportTo(spaceLevel, 0, 10, 0, java.util.Collections.emptySet(), 0, 0);
                        source.sendSuccess(() -> Component.literal("Teleported to Space!"), true);
                        return 1;
                    }
                    source.sendFailure(Component.literal("Space dimension not found!"));
                    return 0;
                })
            )
        );
    }
}
