package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allows entities to receive plot positions
 */
@Mixin(Entity.class)
public abstract class EntityMixin implements EntityStickExtension {

    @Shadow
    private Level level;

    @Shadow
    public abstract void setPos(Vec3 vec3);

    @Shadow
    public abstract void moveTo(Vec3 vec3);

    @Shadow
    public abstract void moveTo(double d, double e, double f);

    @Unique
    private Vec3 sable$plotPosition = null;

    @Inject(method = "tick", at = @At("RETURN"))
    private void sable$updateSubLevelPosition(final CallbackInfo ci) {
        final Entity self = (Entity) (Object) this;

        // non wompy wompy
        if (this.sable$plotPosition != null) {
            final SubLevel subLevel = Sable.HELPER.getContaining(this.level, this.sable$plotPosition);

            if (subLevel != null) {
                this.setPos(subLevel.logicalPose().transformPosition(this.sable$plotPosition));
                ((EntityMovementExtension) this).sable$setTrackingSubLevel(subLevel);
            } else {
                this.sable$plotPosition = null;
            }
        } else if (this.level.isClientSide && !(self instanceof final Player player && player.isLocalPlayer()) && !(self instanceof ItemEntity)) {
            // if we're on the client and the plot position doesn't exist, this must mean the entity was recently
            // networked out of the plot, so let's get rid of the tracking sub-level
            ((EntityMovementExtension) this).sable$setTrackingSubLevel(null);
        }
    }

    @Override
    public void sable$plotLerpTo(final Vec3 pos, final int lerpSteps) {
        this.sable$setPlotPosition(pos);
    }

    @Override
    public void sable$setPlotPosition(@Nullable final Vec3 position) {
        this.sable$plotPosition = position;
    }

    @Override
    public @Nullable Vec3 sable$getPlotPosition() {
        return this.sable$plotPosition;
    }

    @Inject(method = "recreateFromPacket", at = @At("TAIL"))
    public void sable$recreateFromPacket(final ClientboundAddEntityPacket packet, final CallbackInfo ci) {
        if (!EntitySubLevelUtil.shouldKick((Entity) (Object) this)) return;

        final double packetX = packet.getX();
        final double packetY = packet.getY();
        final double packetZ = packet.getZ();

        final SubLevel packetSubLevel = Sable.HELPER.getContaining(this.level, packetX, packetZ);
        if (packetSubLevel != null) {
            final Vector3d globalPacketPos = packetSubLevel.logicalPose().transformPosition(new Vector3d(packetX, packetY, packetZ));
            this.moveTo(globalPacketPos.x, globalPacketPos.y, globalPacketPos.z);
        }
    }
}
