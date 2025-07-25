package net.sixunderscore.mocha2d.util;

public class FpsCounter {
    private static int fpsCount = 0;
    private static float fpsUpdateTimer = 0;

    public static void update() {
        if (fpsUpdateTimer >= 1f) {
            fpsCount = (int) (1f / DeltaTime.getDeltaTime());
            fpsUpdateTimer = 0;
        } else {
            fpsUpdateTimer += DeltaTime.getDeltaTime();
        }
    }

    public static int getCount() {
        return fpsCount;
    }
}
