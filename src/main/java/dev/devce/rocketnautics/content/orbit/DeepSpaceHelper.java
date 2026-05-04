package dev.devce.rocketnautics.content.orbit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.function.UnaryOperator;

public class DeepSpaceHelper {
    public static final Codec<Vector3D> VEC3D_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.DOUBLE.fieldOf("x").forGetter(Vector3D::getX),
                    Codec.DOUBLE.fieldOf("y").forGetter(Vector3D::getY),
                    Codec.DOUBLE.fieldOf("z").forGetter(Vector3D::getZ)
            ).apply(i, Vector3D::new)
    );

    public static final Codec<PVCoordinates> PVCOORDS_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    VEC3D_CODEC.fieldOf("r").forGetter(PVCoordinates::getPosition),
                    VEC3D_CODEC.fieldOf("v").forGetter(PVCoordinates::getVelocity),
                    VEC3D_CODEC.fieldOf("a").forGetter(PVCoordinates::getAcceleration)
            ).apply(i, PVCoordinates::new)
    );

    public static final Codec<TimeOffset> TIME_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.LONG.fieldOf("sec").forGetter(TimeOffset::getSeconds),
                    Codec.LONG.fieldOf("atto").forGetter(TimeOffset::getAttoSeconds)
            ).apply(i, TimeOffset::new)
    );

    public static final Codec<AbsoluteDate> DATE_CODEC = TIME_CODEC.xmap(AbsoluteDate::new, UnaryOperator.identity());

    public static final Codec<TimeStampedPVCoordinates> STAMPED_PVCOORDS_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    DATE_CODEC.fieldOf("d").forGetter(TimeStampedPVCoordinates::getDate),
                    VEC3D_CODEC.fieldOf("r").forGetter(PVCoordinates::getPosition),
                    VEC3D_CODEC.fieldOf("v").forGetter(PVCoordinates::getVelocity),
                    VEC3D_CODEC.fieldOf("a").forGetter(PVCoordinates::getAcceleration)
            ).apply(i, TimeStampedPVCoordinates::new)
    );

    @Nullable
    public static <T> Tag write(@NotNull Codec<T> codec, T obj) {
        return write(codec, obj, null);
    }

    @Nullable
    @Contract("_,_,!null->!null;_,null,_->param3")
    public static <T> Tag write(@NotNull Codec<T> codec, T obj, Tag fallback) {
        if (obj == null) return fallback;
        DataResult<Tag> res = codec.encodeStart(NbtOps.INSTANCE, obj);
        return res.result().orElse(fallback);
    }

    @Nullable
    public static <T> T read(@NotNull Codec<T> codec, Tag tag) {
        return read(codec, tag, null);
    }

    @Nullable
    @Contract("_,_,!null->!null;_,null,_->param3")
    public static <T> T read(@NotNull Codec<T> codec, Tag tag, T fallback) {
        if (tag == null) return fallback;
        DataResult<T> res = codec.parse(NbtOps.INSTANCE, tag);
        return res.result().orElse(fallback);
    }

    public static Vector3d adapt(Vector3D vec) {
        return new Vector3d(vec.getX(), vec.getY(), vec.getZ());
    }

    public static Vector3D adapt(Vector3d vec) {
        return new Vector3D(vec.x, vec.y, vec.z);
    }
}
