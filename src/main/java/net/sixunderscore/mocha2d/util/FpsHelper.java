package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.Window;

public class FpsHelper {
    private final boolean windows;

    // Fps Cap
    private boolean shouldCap;
    private long targetFrameDurationNanos;
    private long nextFrameTimeNanos;
    private float sleepTimePaddingFactor;
    
    // Fps Counter
    private int fpsCount;
    private float fpsCountUpdateTimer;

    public FpsHelper(int fpsCap) {
        this.windows = System.getProperty("os.name").toLowerCase().contains("win");

        this.setFpsCap(fpsCap);
        this.fpsCount = 0;
        this.fpsCountUpdateTimer = 0;

        if (this.windows) {
            // For some reason this improves sleep accuracy on windows
            Thread timerAccuracyThread = new Thread(() -> {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException _) {
                }
            });
            timerAccuracyThread.setDaemon(true);
            timerAccuracyThread.start();
        }
    }

    public void setFpsCap(int fpsCap) {
        if (fpsCap > 0) {
            this.targetFrameDurationNanos = 1_000_000_000L / fpsCap;
            this.nextFrameTimeNanos = System.nanoTime() + this.targetFrameDurationNanos;
            this.sleepTimePaddingFactor = Math.min(1f, MathUtils.lerp(1f, 0.1f, fpsCap / (this.windows ? 500f : 5000f)));
            this.shouldCap = true;
        } else {
            this.shouldCap = false;
        }
    }

    public void cap() {
        if (!this.shouldCap) {
            return;
        }

        if (this.sleepTimePaddingFactor > 0) {
            try {
                long capTimeNanos = Math.max(this.nextFrameTimeNanos - System.nanoTime(), 0);

                // We add a bit of padding in case the thread oversleeps
                long sleepTarget = (long) (capTimeNanos * this.sleepTimePaddingFactor);

                long sleepMillis = sleepTarget / 1_000_000;
                int sleepNanos = (int) (sleepTarget % 1_000_000);

                Thread.sleep(sleepMillis, sleepNanos);
            } catch (InterruptedException _) {
            }
        }

        // If the thread wakes up before the total time is over spin wait the remaining time
        while (System.nanoTime() < this.nextFrameTimeNanos) {
            Thread.onSpinWait();
        }

        this.nextFrameTimeNanos = Math.max(this.nextFrameTimeNanos + this.targetFrameDurationNanos, System.nanoTime() + this.targetFrameDurationNanos);
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
