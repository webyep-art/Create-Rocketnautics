package dev.ryanhcode.sable.command.data_accessor;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;

import java.util.Locale;
import java.util.function.Function;

public class SubLevelDataAccessor implements DataAccessor {
    public static final Function<String, DataCommands.DataProvider> PROVIDER = string -> new DataCommands.DataProvider() {
        @Override
        public DataAccessor access(final CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
            return new SubLevelDataAccessor((ServerSubLevel) SubLevelArgumentType.getSingleSubLevel(commandContext, string));
        }

        @Override
        public ArgumentBuilder<CommandSourceStack, ?> wrap(
                final ArgumentBuilder<CommandSourceStack, ?> argumentBuilder, final Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function
        ) {
            return argumentBuilder.then(
                    Commands.literal("sub_level").then(function.apply(Commands.argument(string, SubLevelArgumentType.singleSubLevel())))
            );
        }
    };
    private final ServerSubLevel subLevel;

    public SubLevelDataAccessor(final ServerSubLevel subLevel) {
        this.subLevel = subLevel;
    }

    @Override
    public void setData(final CompoundTag compoundTag) {
        this.subLevel.setUserDataTag(compoundTag);
    }

    @Override
    public CompoundTag getData() {
        final CompoundTag userTag = this.subLevel.getUserDataTag();
        return userTag != null ? userTag : new CompoundTag();
    }

    @Override
    public Component getModifiedSuccess() {
        return Component.translatable("commands.data.sub_level.modified", this.subLevel.toString());
    }

    @Override
    public Component getPrintSuccess(final Tag tag) {
        return Component.translatable("commands.data.sub_level.query", this.subLevel.toString(), NbtUtils.toPrettyComponent(tag));
    }

    @Override
    public Component getPrintSuccess(final NbtPathArgument.NbtPath nbtPath, final double d, final int i) {
        return Component.translatable(
                "commands.data.sub_level.get", nbtPath.asString(), this.subLevel.toString(), String.format(Locale.ROOT, "%.2f", d), i
        );
    }}
