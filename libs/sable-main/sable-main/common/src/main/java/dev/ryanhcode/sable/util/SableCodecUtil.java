package dev.ryanhcode.sable.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.function.Function;

public class SableCodecUtil {

    public static <N extends Number> Function<N, DataResult<N>> checkPositive(final boolean includeZero) {
        return (value) -> {
            if(includeZero) {
                if(value.doubleValue() < 0) return DataResult.error(() -> "Value " + value + " is not positive or 0");
            } else {
                if(value.doubleValue() <= 0) return DataResult.error(() -> "Value " + value + " is not positive");
            }

            return DataResult.success(value);
        };
    }

    public static Codec<Double> positiveDouble(final boolean includeZero) {
        return Codec.DOUBLE.flatXmap(checkPositive(includeZero), checkPositive(includeZero));
    }


}
