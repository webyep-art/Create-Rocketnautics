package dev.ryanhcode.sable.mixin.respawn_point.sleeping;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Entity {

    public ServerPlayerMixin(final EntityType<?> entityType, final Level level) {
        super(entityType, level);
    }

    /**
     * @author RyanH
     * @reason Sleeping on sub-levels
     */
    @Overwrite
    private boolean isReachableBedBlock(final BlockPos blockPos) {
        final Vec3 bedPos = Vec3.atBottomCenterOf(blockPos);
        Vec3 pos = this.position();

        final SubLevel subLevel = Sable.HELPER.getContaining(this.level(), blockPos);

        if (subLevel != null) {
            pos = subLevel.logicalPose().transformPositionInverse(pos);
        }

        return Math.abs(pos.x - bedPos.x()) <= 3.0 && Math.abs(pos.y - bedPos.y()) <= 2.0 && Math.abs(pos.z - bedPos.z()) <= 3.0;
    }

}
