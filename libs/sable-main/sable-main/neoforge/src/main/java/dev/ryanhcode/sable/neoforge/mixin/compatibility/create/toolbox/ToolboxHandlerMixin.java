package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.toolbox;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import dev.ryanhcode.sable.Sable;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import static com.simibubi.create.content.equipment.toolbox.ToolboxHandler.getMaxRange;

/**
 * Fixes the range check in Create's {@link ToolboxHandler} handling to account for sub-levels.
 */
@Mixin(ToolboxHandler.class)
public abstract class ToolboxHandlerMixin {

    @Shadow
    @Final
    public static WorldAttached<WeakHashMap<BlockPos, ToolboxBlockEntity>> toolboxes;

    @Inject(method = "withinRange", at = @At("HEAD"), remap = false, cancellable = true)
    private static void sable$withinRangeToolBoxRedirect(final Player player, final ToolboxBlockEntity box, final CallbackInfoReturnable<Boolean> cir) {
        if (player.level() != box.getLevel())
            cir.setReturnValue(false);

        final double maxRange = getMaxRange(player);
        cir.setReturnValue(ToolboxHandlerMixin.sable$getDistance(player.level(), player.position(), box.getBlockPos()) < maxRange * maxRange);
    }

    @Inject(method = "getNearest", at = @At("HEAD"), remap = false, cancellable = true)
    private static void sable$getNearestToolBoxRedirect(final LevelAccessor world, final Player player, final int maxAmount, final CallbackInfoReturnable<List<ToolboxBlockEntity>> cir) {
        final Vec3 location = player.position();
        final double maxRange = getMaxRange(player);

        cir.setReturnValue(ToolboxHandlerMixin.toolboxes.get(world)
                .keySet()
                .stream()
                .filter(p -> ToolboxHandlerMixin.sable$getDistance(world, location, p) < maxRange * maxRange)
                .sorted(Comparator.comparingDouble(p -> ToolboxHandlerMixin.sable$getDistance(world, location, p)))
                .limit(maxAmount)
                .map(ToolboxHandlerMixin.toolboxes.get(world)::get)
                .filter(ToolboxBlockEntity::isFullyInitialized)
                .collect(Collectors.toList()));
    }

    @Unique
    private static double sable$getDistance(final LevelAccessor level, final Vec3 pos, final BlockPos bPos) {
        return Sable.HELPER.distanceSquaredWithSubLevels((Level) level, pos, bPos.getX() + 0.5, bPos.getY(), bPos.getZ() + 0.5);
    }

}
