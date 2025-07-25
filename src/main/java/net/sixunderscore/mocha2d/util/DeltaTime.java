package net.sixunderscore.mocha2d.util;

public class DeltaTime {
    private static long lastTime = System.nanoTime();
    private static float deltaTime = 0;

    public static void update() {
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000f;
        lastTime = currentTime;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }
}
