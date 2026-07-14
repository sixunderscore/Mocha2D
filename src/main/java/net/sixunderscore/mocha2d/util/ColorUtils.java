package net.sixunderscore.mocha2d.util;

public class ColorUtils {
    public static float srgbToLinear(byte c) {
        return (float) Math.pow(Byte.toUnsignedInt(c) / 255f, 2.2);
    }
}
