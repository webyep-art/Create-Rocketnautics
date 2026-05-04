package dev.devce.rocketnautics.content.orbit;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ConstantPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.lang.Math;

public class SimplePlanetDefinition implements CubePlanetDefinition {
    protected final @Nullable ResourceKey<Level> localSpace;

    protected final double radius;
    protected final Vector3d rotationAxis;
    protected final int ticksPerRevolution;

    protected final PVCoordinatesProvider positionProvider;
    protected final double mu;
    protected final Frame frame;

    public SimplePlanetDefinition(@Nullable ResourceKey<Level> localSpace, double radius, Vector3d rotationAxis, int ticksPerRevolution, @NotNull Frame parentFrame, @NotNull PVCoordinatesProvider positionProvider, double mu, String frameName) {
        this.localSpace = localSpace;
        this.radius = radius;
        this.rotationAxis = rotationAxis;
        this.ticksPerRevolution = ticksPerRevolution;
        this.positionProvider = positionProvider;
        this.mu = mu;
        this.frame = new Frame(parentFrame, new TransformProvider() {
            @Override
            public Transform getTransform(AbsoluteDate date) {
                TimeStampedPVCoordinates coordsInParentFrame = positionProvider.getPVCoordinates(date, parentFrame);
                return new Transform(date, coordsInParentFrame.negate());
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(FieldAbsoluteDate<T> date) {
                throw new UnsupportedOperationException("Cosmonautics is not configured for arbitrary fields!");
            }
        }, frameName, true);
    }

    @Override
    public @Nullable ResourceKey<Level> localSpaceDimension() {
        return localSpace;
    }

    @Override
    public double radius() {
        return radius;
    }

    @Override
    public Quaterniond rotationAtTime(long universeTime) {
        return new Quaterniond().fromAxisAngleRad(rotationAxis, 2 * Math.PI * universeTime / ticksPerRevolution);
    }

    @Override
    public double getMu() {
        return mu;
    }

    @Override
    public Frame getFrame() {
        return frame;
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
        if (frame == getFrame()) {
            return new TimeStampedPVCoordinates(date, PVCoordinates.ZERO);
        }
        return positionProvider.getPVCoordinates(date, frame);
    }

    public static SimplePlanetDefinitionBuilder builder() {
        return new SimplePlanetDefinitionBuilder();
    }

    public static class SimplePlanetDefinitionBuilder {
        private @Nullable ResourceKey<Level> localSpace;
        private double radius;
        private Vector3d rotationAxis;
        private String frameName;

        private int ticksPerRevolution;
        private boolean tidalLock;

        private double accelerationAtSurface;
        private double mu;

        private CubePlanetDefinition orbited;
        private TimeStampedPVCoordinates orbitCoords;

        private Frame fixedPositionParentFrame;
        private Vector3D fixedPosition;


        public SimplePlanetDefinition build() {
            if (radius <= 0 || frameName == null) {
                throw new IllegalStateException("Builder not fully/properly initialized!");
            }
            Frame frame;
            PVCoordinatesProvider provider;
            if (orbited != null) {
                frame = orbited.getFrame();
                provider = new KeplerianOrbit(orbitCoords, frame, orbited.getMu());
            } else if (fixedPositionParentFrame != null) {
                frame = fixedPositionParentFrame;
                provider = new ConstantPVCoordinatesProvider(fixedPosition, fixedPositionParentFrame);
            } else {
                throw new IllegalStateException("Builder does not have an orbit or a fixed position!");
            }
            // mu / radius^2 = acceleration at surface
            if (mu <= 0) {
                if (accelerationAtSurface <= 0) {
                    throw new IllegalStateException("Builder not fully/properly initialized!");
                }
                mu = accelerationAtSurface * radius * radius;
            }
            if (tidalLock && orbited != null) {
                KeplerianOrbit orbit =  ((KeplerianOrbit) provider);
                double periodSeconds = orbit.getKeplerianPeriod();
                ticksPerRevolution = (int) periodSeconds;
                // TODO check if this needs to be negated to properly tidal lock
                rotationAxis = DeepSpaceHelper.adapt(orbit.getPVCoordinates().getMomentum());
            } else if (ticksPerRevolution <= 0 || rotationAxis == null) {
                throw new IllegalStateException("Builder does not have a revolution time configured!");
            }
            return new SimplePlanetDefinition(localSpace, radius, rotationAxis, ticksPerRevolution, frame, provider, mu, frameName);
        }

        public SimplePlanetDefinitionBuilder setLocalSpace(@Nullable ResourceKey<Level> localSpace) {
            this.localSpace = localSpace;
            return this;
        }

        /**
         * @param radius meters/blocks
         */
        public SimplePlanetDefinitionBuilder setRadius(double radius) {
            this.radius = radius;
            return this;
        }

        public SimplePlanetDefinitionBuilder setRotationAxis(Vector3d rotationAxis) {
            this.rotationAxis = rotationAxis;
            return this;
        }

        public SimplePlanetDefinitionBuilder setTicksPerRevolution(int ticksPerRevolution) {
            this.ticksPerRevolution = ticksPerRevolution;
            return this;
        }

        public SimplePlanetDefinitionBuilder setTidalLocked() {
            if (this.orbited == null) {
                throw new IllegalStateException("Builder does not have an orbit!");
            }
            tidalLock = true;
            return this;
        }

        public SimplePlanetDefinitionBuilder setOrbit(@NotNull CubePlanetDefinition orbited, @NotNull TimeStampedPVCoordinates orbitCoords) {
            this.orbited = orbited;
            this.orbitCoords = orbitCoords;
            return this;
        }

        public SimplePlanetDefinitionBuilder setCircularOrbit(@NotNull CubePlanetDefinition orbited, @NotNull Vector3D position, @NotNull Vector3D orbitAxis) {
            return this.setCircularOrbit(orbited, position, orbitAxis, DeepSpaceData.EPOCH);
        }

        public SimplePlanetDefinitionBuilder setCircularOrbit(@NotNull CubePlanetDefinition orbited, @NotNull Vector3D position, @NotNull Vector3D orbitAxis, @NotNull AbsoluteDate positionDate) {
            this.orbited = orbited;
            double velMagnitudeSquared = this.orbited.getMu() / position.getNorm();
            Vector3D vel = orbitAxis.crossProduct(position);
            vel.scalarMultiply(Math.sqrt(velMagnitudeSquared / vel.getNormSq()));
            this.orbitCoords = new TimeStampedPVCoordinates(positionDate, position, vel);
            return this;
        }

        public SimplePlanetDefinitionBuilder setFixedPosition(@NotNull Frame positionFrame, @NotNull Vector3D position) {
            this.fixedPositionParentFrame = positionFrame;
            this.fixedPosition = position;
            return this;
        }

        /**
         * @param accelerationAtSurface meters per second squared
         */
        public SimplePlanetDefinitionBuilder setAccelerationAtSurface(double accelerationAtSurface) {
            this.accelerationAtSurface = accelerationAtSurface;
            return this;
        }

        /**
         * @param mu meters cubed per second squared
         */
        public SimplePlanetDefinitionBuilder setMu(double mu) {
            this.mu = mu;
            return this;
        }

        public SimplePlanetDefinitionBuilder setFrameName(String frameName) {
            this.frameName = frameName;
            return this;
        }
    }
}
