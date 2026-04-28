package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

public class SableFlywheelMatrixBuffer {
    public static final int INFO_SIZE_BYTES = (16 + 12) * Float.BYTES +
            Float.BYTES + // sky light scale
            Integer.BYTES + // scene ID
            2 * Float.BYTES + // padding
            16 * Float.BYTES; // scene matrix
}
