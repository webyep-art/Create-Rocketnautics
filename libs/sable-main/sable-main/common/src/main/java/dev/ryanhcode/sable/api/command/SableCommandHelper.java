package dev.ryanhcode.sable.api.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;

public class SableCommandHelper {

    private static final SimpleCommandExceptionType MISSING_SUBLEVEL_CONTAINER
            = new SimpleCommandExceptionType(Component.translatable("commands.sable.helper.missing_sub_level_container"));
    private static final SimpleCommandExceptionType MISSING_PHYSICS_SYSTEM
            = new SimpleCommandExceptionType(Component.translatable("commands.sable.helper.missing_physics_system"));
    public static final SimpleCommandExceptionType ERROR_NO_SUB_LEVELS_FOUND = new SimpleCommandExceptionType(Component.translatable("commands.sable.fail.no_sub_levels"));
    public static final SimpleCommandExceptionType ERROR_NOT_INSIDE_SUB_LEVEL = new SimpleCommandExceptionType(Component.translatable("commands.sable.fail.not_inside_sub_level"));
    public static final SimpleCommandExceptionType ERROR_NO_AXIS_FOR_ROTATION = new SimpleCommandExceptionType(Component.translatable("commands.sable.fail.no_axis_for_rotation"));
    public static final SimpleCommandExceptionType ERROR_NO_SUB_LEVELS_MODIFIED = new SimpleCommandExceptionType(Component.translatable("commands.sable.fail.unmodified"));
    public static final SimpleCommandExceptionType ERROR_SUB_LEVEL_UNNAMED = new SimpleCommandExceptionType(Component.translatable("commands.sable.sub_level.get_name.failure_unnamed"));

    // Component utilities related to sub-levels

    /**
     * Returns a formatted component, with one of the arguments describing the subLevels parameter, being either:
     * <ul>
     *     <li>The name of the data or "sub-level" if there is only one sub-level</li>
     *     <li>The number of sub-levels in the collection if there are multiple</li>
     * </ul>
     *
     * @param translationKey            The translation key to use
     * @param subLevels                 The collection of sub-levels to describe
     * @param subLevelsDescriptionIndex The index of the sub-levels description in the args array
     * @param additionalArguments       The additional arguments to pass to the translation key
     */
    public static Component getResultComponentForSublevelCollection(final String translationKey, final Collection<ServerSubLevel> subLevels,
                                                                     final int subLevelsDescriptionIndex, final Object... additionalArguments) {
        final boolean isPlural = subLevels.size() != 1;

        // Varargs of an Object type don't handle arrays so it has to be manually collected
        final Object[] translationArguments = new Object[additionalArguments.length + 1];
        System.arraycopy(additionalArguments, 0, translationArguments, 1, additionalArguments.length);

        if (isPlural) {
            translationArguments[0] = Component.translatable("commands.sable.sub_levels", subLevels.size());
        } else {
            final SubLevel subLevel = subLevels.iterator().next();
            final Object name = subLevel.getName() == null ? Component.translatable("commands.sable.sub_level") : subLevel.getName();
            translationArguments[0] = name;
        }

        if (subLevelsDescriptionIndex != 0) {
            final Object swap = translationArguments[subLevelsDescriptionIndex];
            translationArguments[subLevelsDescriptionIndex] = translationArguments[0];
            translationArguments[0] = swap;
        }

        return Component.translatable(translationKey, translationArguments);
    }

    /**
     * Sends a formatted component, where the specified translation key is given a description of the sub-levels collection</br>
     * See {@link SableCommandHelper#getResultComponentForSublevelCollection} for more info about the description
     * <i>Functionally an overload of {@link SableCommandHelper#getResultComponentForSublevelCollection}, but with a different name due to the Object varargs.<i/>
     * @param translationKey            The translation key to use
     * @param context                   The command context to send the message to
     * @param subLevels                 The collection of sub-levels to describe
     * @param additionalArguments       The additional arguments to pass to the translation key
     */
    public static void sendSuccessDescribingSubLevels(final String translationKey, final CommandContext<CommandSourceStack> context, final Collection<ServerSubLevel> subLevels,
                                                      final Object... additionalArguments) {
        sendSuccessDescribingSubLevelsAtIndex(translationKey, context, subLevels, 0, additionalArguments);
    }

    /**
     * Sends a formatted component, where the specified translation key is given a description of the sub-levels collection, in the index specified</br>
     * See {@link SableCommandHelper#getResultComponentForSublevelCollection} for more info about the description
     * @param translationKey            The translation key to use
     * @param context                   The command context to send the message to
     * @param subLevels                 The collection of sub-levels to describe
     * @param subLevelsDescriptionIndex The index of the sub-levels description in the args array
     * @param additionalArguments       The additional arguments to pass to the translation key
     */
    public static void sendSuccessDescribingSubLevelsAtIndex(final String translationKey, final CommandContext<CommandSourceStack> context, final Collection<ServerSubLevel> subLevels,
                                                             final int subLevelsDescriptionIndex, final Object... additionalArguments) {
        context.getSource().sendSuccess(
                () -> getResultComponentForSublevelCollection(translationKey, subLevels, subLevelsDescriptionIndex, additionalArguments),
                true
        );
    }

    //Requires with a command exception
    
    public static ServerSubLevelContainer requireSubLevelContainer(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return requireSubLevelContainer(context.getSource());
    }

    public static ServerSubLevelContainer requireSubLevelContainer(final CommandSourceStack source) throws CommandSyntaxException {
        final ServerLevel level = source.getLevel();
        return requireNotNull(SubLevelContainer.getContainer(level), MISSING_SUBLEVEL_CONTAINER);
    }

    public static SubLevelPhysicsSystem requireSubLevelPhysicsSystem(final ServerSubLevelContainer subLevelContainer) throws CommandSyntaxException {
        return requireNotNull(subLevelContainer.physicsSystem(), MISSING_PHYSICS_SYSTEM);
    }

    //Overloads from context only

    public static SubLevelPhysicsSystem requireSubLevelPhysicsSystem(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return requireSubLevelPhysicsSystem(
                requireSubLevelContainer(context)
        );
    }
    
    public static PhysicsPipeline requireSubLevelPhysicsPipeline(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return requireSubLevelPhysicsSystem(context).getPipeline();
    }
    
    //
    
    public static <T> T requireNotNull(final T value, final SimpleCommandExceptionType message) throws CommandSyntaxException {
        if (value == null) {
            throw message.create();
        }
        return value;
    }

}
