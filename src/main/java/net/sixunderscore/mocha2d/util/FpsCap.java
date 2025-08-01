package net.sixunderscore.mocha2d.util;

import java.util.concurrent.locks.LockSupport;

public class FpsCap {
    private final boolean shouldCap;
    private final long frameCapDurationNanos;
    private long nextFrameTimeNanos;

    public FpsCap(int fpsCap) {
        if (fpsCap > 0) {
            this.frameCapDurationNanos = 1_000_000_000L / fpsCap;
            this.nextFrameTimeNanos = System.nanoTime();
            this.shouldCap = true;
        } else {
            this.frameCapDurationNanos = 0;
            this.shouldCap = false;
        }
    }

    public void cap() {
        if (!this.shouldCap) {
            return;
        }

        while ((this.nextFrameTimeNanos - System.nanoTime()) > 0) {
            LockSupport.parkNanos(1);
        }

        this.nextFrameTimeNanos = Math.max(this.nextFrameTimeNanos + this.frameCapDurationNanos, System.nanoTime());
    }
}

