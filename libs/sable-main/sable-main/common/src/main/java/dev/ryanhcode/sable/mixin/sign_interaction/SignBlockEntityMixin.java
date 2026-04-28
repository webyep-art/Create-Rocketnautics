package dev.ryanhcode.sable.mixin.sign_interaction;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Fixes the method that determines if the player interacts with the front or back of a sign to take sub-levels into account.
 */
@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity {

    public SignBlockEntityMixin(final BlockEntityType<?> blockEntityType, final BlockPos blockPos, final BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    /**
     * @author RyanH
     * @reason Overwrite to fix sign face interaction
     */
    @Overwrite
    public boolean isFacingFrontText(final Player player) {
        final BlockState state = this.getBlockState();
        final Block block = state.getBlock();
        if (block instanceof final SignBlock signBlock) {
            final ActiveSableCompanion helper = Sable.HELPER;
            final BlockPos pos = this.getBlockPos();
            final Vector3d signCenterPos = JOMLConversion.toJOML(signBlock.getSignHitboxCenterPosition(state).add(pos.getX(), pos.getY(), pos.getZ()));
            final Vector3d center = helper.projectOutOfSubLevel(this.level, signCenterPos);
            final Vector3d deltaDir = JOMLConversion.toJOML(player.position()).sub(center).normalize();

            final float signYRot = signBlock.getYRotationDegrees(state);
            final Vector3d signNormal = new Vector3d(0.0, 0.0, 1.0).rotateY(Math.toRadians(-signYRot));

            final SubLevel subLevel = helper.getContaining(this.level, pos);

            if (subLevel != null) {
                subLevel.logicalPose().transformNormal(signNormal);
            }

            return signNormal.dot(deltaDir.x, deltaDir.y, deltaDir.z) > 0.0;
        } else {
            return false;
        }
    }

}
