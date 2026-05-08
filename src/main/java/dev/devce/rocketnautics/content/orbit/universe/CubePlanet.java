package dev.devce.rocketnautics.content.orbit.universe;

import dev.devce.rocketnautics.client.PlanetColors;
import dev.devce.rocketnautics.content.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.content.orbit.FrameTree;
import dev.devce.rocketnautics.content.orbit.FrameTreeOwner;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;

import java.util.function.IntFunction;

// note -- local space dimension and render data supplier are never synced to client
public record CubePlanet(@NotNull FrameTree frame, double radius, TimeStampedAngularCoordinates rotationDescription,
                         @Nullable ResourceKey<Level> localSpaceDimension, @Nullable IntFunction<byte[]> renderDataSupplier) implements FrameTreeOwner {

    public byte[] getRenderData(int powerScaleClamp) {
        if (renderDataSupplier == null) return PlanetColors.BLANK;
        return renderDataSupplier.apply(powerScaleClamp);
    }

    public Rotation getRotationAtTime(AbsoluteDate date) {
        return rotationDescription.rotationShiftedBy(date.durationFrom(rotationDescription.getDate()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(frame.getId());
        buf.writeDouble(radius);
        DeepSpaceHelper.STAMPED_ANGULARCOORDS_CODEC_S.encode(buf, rotationDescription);
    }

    public static CubePlanet read(FriendlyByteBuf buf, FrameTree frameSource) {
        int id = buf.readVarInt();
        double radius = buf.readDouble();
        TimeStampedAngularCoordinates coords = DeepSpaceHelper.STAMPED_ANGULARCOORDS_CODEC_S.decode(buf);
        return new CubePlanet(frameSource.getInTreeByID(id).get(), radius, coords, null, null);
    }
}
