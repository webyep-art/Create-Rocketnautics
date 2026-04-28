package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.nozzles;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.neoforge.mixin.compatibility.create.nozzle.NozzleBlockEntityAccessor;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NozzleHoveringHelper {

	public static List<Couple<Vec3>> gatherRaycastPoints(final BlockState state) {
		if (state.hasProperty(BlockStateProperties.FACING)) {
			final List<Couple<Vec3>> rayPoints = new ArrayList<>();

			final Direction facing = state.getValue(BlockStateProperties.FACING);
			final Direction startingDir;

			if (facing.getAxis() == Direction.Axis.Y) {
				startingDir = Direction.NORTH;
			} else {
				startingDir = facing.getClockWise();
			}

			final int horizontalSamplePoints = 6;
			final int theta = 360 / horizontalSamplePoints;

			final double startScaling = 0.8;
			final double endScaling = 8;
			for (final boolean diagonal : Iterate.trueAndFalse) {
				for (int i = 0; i < horizontalSamplePoints; i++) {
					Vec3 start = Vec3.atLowerCornerOf(startingDir.getNormal()).scale(startScaling).add(0.5, 0.5, 0.5);
					if (diagonal) {
						final Direction.Axis axis;
						final double angle;
						if (facing.getAxis().isHorizontal()) {
							axis = Direction.Axis.Y;
							angle = 45;
						} else {
							axis = startingDir.getClockWise().getAxis();
							angle = facing.getAxisDirection().getStep() * 45;
						}

						start = VecHelper.rotateCentered(start, angle, axis);
					}

					start = VecHelper.rotateCentered(start, theta * i, facing.getAxis());
					final Vec3 end = start.add(start.subtract(0.5, 0.5, 0.5).scale(endScaling));
					rayPoints.add(Couple.create(start, end));
				}
			}

			final Vec3 start = Vec3.atLowerCornerOf(facing.getNormal()).scale(startScaling).add(0.5, 0.5, 0.5);
			final Vec3 end = start.add(start.subtract(0.5, 0.5, 0.5).scale(endScaling));
			rayPoints.add(Couple.create(start, end));

			return rayPoints;
		}

		return null;
	}

	@Nullable
	public static Vector3d gatherForceFromRays(final SubLevel parentSublevel, final double timeStep, final Level level, final BlockPos blockStart, final NozzleBlockEntity nbe, final List<Couple<Vec3>> rayPoints) {
		if (((NozzleBlockEntityAccessor) nbe).getRange() == 0) {
			return null;
		}

		final Optional<EncasedFanBlockEntity> be = level.getBlockEntity(blockStart.relative(nbe.getBlockState().getValue(BlockStateProperties.FACING).getOpposite()), AllBlockEntityTypes.ENCASED_FAN.get());
		if (be.isPresent()) {
			final EncasedFanBlockEntity fbe = be.get();
			final Vector3d force = new Vector3d();

			final Couple<Vec3> firstRay = rayPoints.getFirst();
			final double startEndDistance = firstRay.getSecond().subtract(firstRay.getFirst()).length();
			final Vec3 blockCorner = Vec3.atLowerCornerOf(blockStart);

			for (final Couple<Vec3> rayPoint : rayPoints) {
				final Vec3 start = blockCorner.add(rayPoint.getFirst());
				final Vec3 end = blockCorner.add(rayPoint.getSecond());

				final ClipContext context = new ClipContext(
						start,
						end,
						ClipContext.Block.OUTLINE,
						ClipContext.Fluid.ANY,
						CollisionContext.empty()
				);

				final BlockHitResult clip = level.clip(context);
				if (clip.getType() == HitResult.Type.MISS) {
					continue;
				}

                final ActiveSableCompanion helper = Sable.HELPER;
                final SubLevel hitSublevel = helper.getContaining(level, clip.getBlockPos());
				if (hitSublevel == parentSublevel) {
					continue;
				}

				final Vec3 hitDiff = helper.projectOutOfSubLevel(level, clip.getLocation())
						.subtract(helper.projectOutOfSubLevel(level, start));

				final double inverseHitPercentage;
				if (clip.isInside()) {
					inverseHitPercentage = 1;
				} else {
					final float curveScaling = 2f; //2 == full power at 1/2 distance, 1 == full power at 0 distance

					inverseHitPercentage = Math.clamp(curveScaling - ((hitDiff.length() / startEndDistance) * curveScaling), 0, 1);
				}

				final Vec3 modifiedDiff = hitDiff
						.normalize()
						.scale(inverseHitPercentage)
						.scale(1d / rayPoints.size());
				force.add(modifiedDiff.x, modifiedDiff.y, modifiedDiff.z);

				if (hitSublevel instanceof final ServerSubLevel hitServerSubLevel) {
					final ForceTotal forceTotal = hitServerSubLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).getForceTotal();
					final Vector3d impulseLocation = JOMLConversion.toJOML(clip.getLocation());
					final Vector3d impulse = hitServerSubLevel.logicalPose().transformNormalInverse(JOMLConversion.toJOML(modifiedDiff)).mul(-1).mul(getFanMagnitudeCalculation(parentSublevel, level, fbe) * timeStep);
					forceTotal.applyImpulseAtPoint(hitServerSubLevel.getMassTracker(), impulseLocation, impulse);
				}
			}

			if (force.length() > 1e-8) {
				force.mul(getFanMagnitudeCalculation(parentSublevel, level, fbe) * timeStep);
				parentSublevel.logicalPose().transformNormalInverse(force);
			}

			return force;
		}

		return null;
	}

	private static double getFanMagnitudeCalculation(final SubLevel parentSublevel, final Level level, final EncasedFanBlockEntity fbe) {
		final float scale = fbe.getBlockState().getValue(EncasedFanBlock.FACING).getAxisDirection() == Direction.AxisDirection.POSITIVE ? -1 : 1;
		final double airPressure = DimensionPhysicsData.getAirPressure(level, parentSublevel.logicalPose().transformPosition(JOMLConversion.atCenterOf(fbe.getBlockPos())));

		final int magnitude = 5;
		final int softScaling = 4; //higher == less impact above 128 RPM

		final float signumBefore = Math.signum(fbe.getSpeed());
		float speed = Math.abs(fbe.getSpeed());

		final int maxSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get();
		final float halfSpeed = maxSpeed / 2f;
		if (speed >= halfSpeed) {
			speed = ((speed - halfSpeed) / softScaling) + halfSpeed;
		}

		speed *= signumBefore;
		return magnitude * scale * speed * airPressure;
	}

	public static void spawnWindHitParticle(final Level level, final SubLevel subLevel, final BlockHitResult clip, final Vector3dc origin, final double airSpeed) {
		final Vector3d end = JOMLConversion.toJOML(clip.getLocation());

		if (clip.getType() != HitResult.Type.MISS && origin.distanceSquared(end.x, end.y, end.z) > 1) {
			final BlockState hitState = level.getBlockState(clip.getBlockPos());
			final Fluid fluid = level.getFluidState(clip.getBlockPos()).getType();

			final Vector3d start = new Vector3d(origin);

			if (subLevel != null) {
				subLevel.logicalPose().transformPosition(start);
			}
			final Vector3d normal = new Vector3d(clip.getDirection().getStepX(), clip.getDirection().getStepY(), clip.getDirection().getStepZ());
			final SubLevel other = Sable.HELPER.getContaining(level, clip.getBlockPos());
			if (other != null) {
				other.logicalPose().transformNormal(normal);
				other.logicalPose().transformPosition(end);
			}

			final Vector3d offset = new Vector3d(
					level.random.nextDouble() * 2 - 1,
					level.random.nextDouble() * 2 - 1,
					level.random.nextDouble() * 2 - 1
			);
			projectOntoPlane(offset, normal, 1);
			end.add(offset);
			final Vector3d delta = end.sub(start, new Vector3d());

			final Vector3d particleVelocity = projectOntoPlane(new Vector3d(delta), normal, 1);
			particleVelocity.mul(airSpeed);
			particleVelocity.fma(0.25, normal);
			end.fma(0.1, normal);

			if (other != null)
				other.logicalPose().orientation().transformInverse(particleVelocity);

			level.addParticle(ParticleTypes.DUST_PLUME, end.x, end.y, end.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
			if (hitState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
				level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, hitState), end.x, end.y, end.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
			} else if (fluid.isSame(Fluids.WATER)) {
				level.addParticle(ParticleTypes.SPLASH, end.x, end.y, end.z, 0, 0, 0);
				if (level.getRandom().nextDouble() < 0.2)
					level.addParticle(ParticleTypes.BUBBLE, end.x, end.y, end.z, 0, 0, 0);

			} else if (fluid.isSame(Fluids.LAVA)) {
				level.addParticle(ParticleTypes.SMOKE, end.x, end.y, end.z, 0, 0, 0);
				if (level.getRandom().nextDouble() < 0.2)
					level.addParticle(ParticleTypes.LAVA, end.x, end.y, end.z, 0, 0, 0);

			}
		}
	}

	private static Vector3d projectOntoPlane(final Vector3d x, final Vector3dc planeNormal, final double scale) {
		return x.fma(-scale * x.dot(planeNormal), planeNormal);
	}
}
