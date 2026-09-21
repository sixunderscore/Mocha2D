package net.sixunderscore.mocha2d.util;

public class MiscUtils {
    public static final int MATRIX_4F_BYTES = 16 * Float.BYTES;
    public static final int MATRIX_2F_BYTES = 4 * Float.BYTES;

    public static float srgbToLinearColor(byte c) {
        return (float) Math.pow(Byte.toUnsignedInt(c) / 255f, 2.2);
    }
}
