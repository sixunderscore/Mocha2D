package net.sixunderscore.mocha2d.util;

public class MathUtils {
    public static final float PI_f = (float) Math.PI;
    public static final float TAU_f = (float) Math.TAU;
    public static final float PI_OVER_2 = PI_f / 2.0f;

    private static final float[] sinTable = createSinTable();
    private static final int TABLE_SIZE = 1024;
    private static final float sinTableSizeOverPi2 = TABLE_SIZE / TAU_f;
    private static final int lookupTableMask = TABLE_SIZE - 1;

    private static float[] createSinTable() {
        float[] table = new float[TABLE_SIZE];
        float step = TAU_f / TABLE_SIZE;
        float theta = 0;

        for (int i = 0; i < TABLE_SIZE; ++i) {
            table[i] = (float) Math.sin(theta);
            theta += step;
        }

        return table;
    }

    public static float lookupCos(float a) {
        return lookupSin(a + PI_OVER_2);
    }

    public static float lookupSin(float a) {
        float index = a * sinTableSizeOverPi2;
        int intIndex = (int) index;
        int i = intIndex & lookupTableMask;

        return lerp(sinTable[i], sinTable[(i + 1) & lookupTableMask], index - intIndex);
    }

    /*
     * https://math.stackexchange.com/questions/1098487/atan2-faster-approximation
     * */
    public static float fastAtan2(float y, float x) {
        float ax = Math.abs(x);
        float ay = Math.abs(y);
        float a = Math.min(ax, ay) / Math.max(ax, ay);
        float s = a * a;
        float r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a);

        if (ay > ax) {
            r = PI_OVER_2 - r;
        }
        if (x < 0) {
            r = PI_f - r;
        }

        return y < 0 ? -r : r;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
