package dev.devce.rocketnautics.content.orbit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.hipparchus.analysis.solvers.BrentSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Collection;

public final class DeepSpaceInstance {
    private static final Orbit FALLBACK = new CartesianOrbit(new TimeStampedPVCoordinates(DeepSpaceData.EPOCH, Vector3D.PLUS_I, Vector3D.PLUS_J), Frame.getRoot(), 1);
    private static final BrentSolver SOLVER = new BrentSolver();

    private final DeepSpaceData manager;
    private final int sideLength;
    private final int negXCorner;
    private final int negZCorner;
    private final int id;

    private @NotNull Orbit currentOrbit = FALLBACK;
    private int timescale = 1;
    private long localUniverseTicks;

    public DeepSpaceInstance(DeepSpaceData manager, int sideLength, int negXCorner, int negZCorner, int id) {
        this.manager = manager;
        this.sideLength = sideLength;
        this.negXCorner = negXCorner;
        this.negZCorner = negZCorner;
        this.id = id;
        this.localUniverseTicks = manager.getUniverseTicks();
    }

    public DeepSpaceInstance(DeepSpaceData manager, CompoundTag tag) {
        this.manager = manager;
        this.sideLength = tag.getInt("SideLength");
        this.negXCorner = tag.getInt("NegX");
        this.negZCorner = tag.getInt("NegZ");
        this.id = tag.getInt("Id");
        this.localUniverseTicks = tag.getLong("LocalTicks");
        Frame frame = manager.getUniverse().getFrameByName(tag.getString("Frame"));
        TimeStampedPVCoordinates coords = DeepSpaceHelper.read(DeepSpaceHelper.STAMPED_PVCOORDS_CODEC, tag.get("Coords"));
        if (frame != null && coords != null) {
            PointGravitySource controlling = determineControllingGravitySource(coords, frame);
            this.currentOrbit = createOrbitAroundSource(coords, frame, controlling);
        }
    }

    // cannot be codec-driven due to the need for the DeepSpaceData object during deserialization.
    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SideLength", sideLength);
        tag.putInt("NegX", negXCorner);
        tag.putInt("NegZ", negZCorner);
        tag.putInt("Id", id);
        tag.putLong("LocalTicks", localUniverseTicks);
        tag.putString("Frame", currentOrbit.getFrame().getName());
        Tag c = DeepSpaceHelper.write(DeepSpaceHelper.STAMPED_PVCOORDS_CODEC, currentOrbit.getPVCoordinates());
        if (c != null) tag.put("Coords", c);
        return tag;
    }

    public boolean isCorrupted() {
        return currentOrbit == FALLBACK;
    }

    public int getId() {
        return id;
    }

    public int getSideLength() {
        return sideLength;
    }

    public int getNegXCorner() {
        return negXCorner;
    }

    public int getNegZCorner() {
        return negZCorner;
    }

    public int getTimescale() {
        return timescale;
    }

    public void setTimescale(int timescale) {
        this.timescale = timescale;
    }

    public void tick() {
        localUniverseTicks += timescale;
        // check if we have entered the domain of a different gravity source;
        // if so, find out exactly when and update our orbit.
        Frame currentFrame = currentOrbit.getFrame();
        // should we do a sanity check to see if our previous position is in the old gravity source's domain?
        // if the universe is immutable during runtime, which it should be, that shouldn't be necessary.
        PointGravitySource controlling = determineControllingGravitySource(currentOrbit.getPVCoordinates(DeepSpaceData.getTime(localUniverseTicks), currentFrame), currentFrame);
        if (controlling.getFrame() != currentOrbit.getFrame()) {
            localUniverseTicks -= timescale;
            // between the two bodies, there is a "transition plane" where their induced acceleration is equivalent.
            // mu(obj1) / d2(obj1.r(t), r(t)) = mu(obj2) / d2(obj2.r(t), r(t))
            // equivalently, mu(obj2) * d2(obj1.r(t), r(t)) = mu(obj1) * d2(obj2.r(t), r(t))
            // we want to find the point on the current orbit that intersects this plane.
            // we know the point is between the time we were at and the time we are now at.
            // we solve this numerically via Brent's Method.
            AbsoluteDate startTime = DeepSpaceData.getTime(localUniverseTicks);
            // our level of accuracy is controlled by the maximum evaluations.
            // would it be better to instead set an absolute accuracy of 0.5,
            // then round the solved time to the nearest tick?
            double transitionTicks = SOLVER.solve(10, t -> {
                double mu1 = currentOrbit.getMu();
                double mu2 = controlling.getMu();
                AbsoluteDate time = startTime.shiftedBy(t / 20); // convert from floating ticks to seconds
                return mu2 * currentOrbit.getPosition(time, currentFrame).getNormSq() // our position in our orbital frame
                        - mu1 * currentOrbit.getPosition(time, controlling.getFrame()).getNormSq(); // our position in their orbital frame
            }, 0, timescale);
            AbsoluteDate transitionTime = startTime.shiftedBy(transitionTicks / 20);
            // construct a new orbit, starting from our position at the transition time.
            TimeStampedPVCoordinates transitionCoords = currentOrbit.getPVCoordinates(transitionTime, controlling.getFrame());
            currentOrbit = createOrbitAroundSource(transitionCoords, controlling.getFrame(), controlling);
            localUniverseTicks += timescale;
        }
    }

    private PointGravitySource determineControllingGravitySource(TimeStampedPVCoordinates coords, Frame frame) {
        Collection<PointGravitySource> sources = manager.getUniverse().getGravitySources();
        PointGravitySource strongest = null;
        double strongestAcceleration = 0;
        // the distance calculation doesn't need to be super accurate, but it does need to be fast.
        // thus we compute all distances in the root frame.
        Vector3D position = frame.getStaticTransformTo(Frame.getRoot(), coords.getDate()).transformPosition(coords.getPosition());
        for (PointGravitySource source : sources) {
            Vector3D bodyPosition = source.getPosition(coords.getDate(), Frame.getRoot());
            double accel = source.getMu() / position.distanceSq(bodyPosition);
            if (accel > strongestAcceleration) {
                strongest = source;
                strongestAcceleration = accel;
            }
        }
        return strongest;
    }

    private Orbit createOrbitAroundSource(TimeStampedPVCoordinates currentCoords, Frame currentFrame, PointGravitySource source) {
        // revise coordinates to be in the gravity source's frame.
        if (currentFrame != source.getFrame()) {
            currentCoords = currentFrame.getTransformTo(source.getFrame(), currentCoords.getDate()).transformPVCoordinates(currentCoords);
        }
        return new CartesianOrbit(currentCoords, source.getFrame(), source.getMu());
    }

    public AbsoluteDate getLocalUniverseTime() {
        return DeepSpaceData.getTime(localUniverseTicks);
    }

}
