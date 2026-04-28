package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.redstone_links;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Changes the range checks between links in in Create's {@link RedstoneLinkNetworkHandler} to take into account sub-levels
 * TODO: Handle movement when signals are active
 */
@Mixin(RedstoneLinkNetworkHandler.class)
public class RedstoneLinkNetworkHandlerMixin {

    @Redirect(
            method = "updateNetworkOf",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange(Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z"),
            remap = false)
    private boolean sable$projectComparisons(final IRedstoneLinkable from, final IRedstoneLinkable to, @Local(argsOnly = true) final LevelAccessor levelAccessor) {
        final Level level = (Level) levelAccessor;

        if (from == to) return true;

        final Vector3d fromPos = JOMLConversion.atCenterOf(from.getLocation());
        final Vector3d toPos = JOMLConversion.atCenterOf(to.getLocation());

        final ActiveSableCompanion helper = Sable.HELPER;
        final SubLevel fromSublevel = helper.getContaining(level, fromPos);
        if (fromSublevel != null) {
            fromSublevel.logicalPose().transformPosition(fromPos);
        }

        final SubLevel toSublevel = helper.getContaining(level, toPos);
        if (toSublevel != null) {
            toSublevel.logicalPose().transformPosition(toPos);
        }

        final int linkRange = AllConfigs.server().logistics.linkRange.get();
        return fromPos.distanceSquared(toPos) < linkRange * linkRange;
    }
}
