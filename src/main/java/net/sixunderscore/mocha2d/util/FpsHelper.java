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
            this.nextFrameTimeNanos = System.nanoTime() + this.frameCapDurationNanos;
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

        // Sleep 85% of the total time to add a bit of padding in case the thread oversleeps
        try {
            long capTimeNanos = Math.max(this.nextFrameTimeNanos - System.nanoTime(), 0);
            long sleepTarget = (long) (capTimeNanos * 0.85);

            long sleepMillis = sleepTarget / 1_000_000;
            int sleepNanos = (int) (sleepTarget % 1_000_000);

            Thread.sleep(sleepMillis, sleepNanos);
        } catch (InterruptedException ignored) {
        }

        // If the thread wakes up before the total time is over spin wait the remaining time
        while (System.nanoTime() < this.nextFrameTimeNanos) {
            Thread.onSpinWait();
        }

        this.nextFrameTimeNanos = Math.max(
            nextFrameTimeNanos + frameCapDurationNanos, 
            System.nanoTime() + frameCapDurationNanos
        );
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

