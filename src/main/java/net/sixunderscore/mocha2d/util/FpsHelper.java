package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.Window;

public class FpsHelper {
    // Fps Cap
    private final boolean shouldCap;
    private final long frameCapDurationNanos;
    private long nextFrameTimeNanos;
    // Fps Counter
    private int fpsCount;
    private float fpsCountUpdateTimer;

    public FpsHelper(int fpsCap) {
        if (fpsCap > 0) {
            this.frameCapDurationNanos = 1_000_000_000L / fpsCap;
            this.nextFrameTimeNanos = System.nanoTime();
            this.shouldCap = true;
        } else {
            this.frameCapDurationNanos = 0;
            this.shouldCap = false;
        }

        this.fpsCount = 0;
        this.fpsCountUpdateTimer = 0;
    }

    public void cap() {
        if (!this.shouldCap) {
            return;
        }

        while ((this.nextFrameTimeNanos - System.nanoTime()) > 0) {
            Thread.onSpinWait();
        }

        this.nextFrameTimeNanos = Math.max(this.nextFrameTimeNanos + this.frameCapDurationNanos, System.nanoTime());
    }

    public void updateCount() {
        if (this.fpsCountUpdateTimer >= 1f) {
            this.fpsCount = (int) (1f / Window.getDeltaTime());
            this.fpsCountUpdateTimer = 0;
        } else {
            this.fpsCountUpdateTimer += Window.getDeltaTime();
        }
    }

    public int getCount() {
        return this.fpsCount;
    }
}

