package net.sixunderscore.mocha2d.util;

public class FpsCap {
    private final boolean shouldCap;
    private long frameCapDurationNanos;
    private long nextFrameTimeNanos;

    public FpsCap(int fpsCap) {
        if (fpsCap > 0) {
            this.frameCapDurationNanos = 1_000_000_000L / fpsCap;
            this.nextFrameTimeNanos = System.nanoTime();
            this.shouldCap = true;
        } else {
            this.shouldCap = false;
        }
    }

    public void cap() {
        if (!this.shouldCap) {
            return;
        }

        try {
            long now = System.nanoTime();

            while ((this.nextFrameTimeNanos - now) > 0) {
                Thread.sleep(0, 1);
                now = System.nanoTime();
            }
        } catch (InterruptedException ignored) {}

        this.nextFrameTimeNanos = Math.max(this.nextFrameTimeNanos + this.frameCapDurationNanos, System.nanoTime());
    }
}

