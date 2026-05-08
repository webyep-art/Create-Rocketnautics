package dev.devce.rocketnautics.content.orbit.universe;

import dev.devce.rocketnautics.content.orbit.FrameTree;
import dev.devce.rocketnautics.content.orbit.FrameTreeOwner;
import net.minecraft.network.FriendlyByteBuf;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

public record PointGravitySource(@NotNull FrameTree frame, double mu, double roi) implements PVCoordinatesProvider, FrameTreeOwner {

    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        return orekitFrame().getStaticTransformTo(frame, date).transformPosition(Vector3D.ZERO);
    }

    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
        final PVCoordinates pvCoordinates = orekitFrame().getKinematicTransformTo(frame, date).transformOnlyPV(PVCoordinates.ZERO);
        return pvCoordinates.getVelocity();
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
        PVCoordinates coords = orekitFrame().getTransformTo(frame, date).transformPVCoordinates(PVCoordinates.ZERO);
        return new TimeStampedPVCoordinates(date, coords);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(frame.getId());
        buf.writeDouble(mu);
        buf.writeDouble(roi);
    }

    public static PointGravitySource read(FriendlyByteBuf buf, FrameTree frameSource) {
        int id = buf.readVarInt();
        double mu = buf.readDouble();
        double roi = buf.readDouble();
        return new PointGravitySource(frameSource.getInTreeByID(id).get(), mu, roi);
    }
}
