package net.sixunderscore.mocha2d.util;

import java.util.Arrays;

public class AverageCalculator {
    private final long[] slots;
    private int offset;

    public AverageCalculator(int size, long initialValue) {
        this.slots = new long[size];
        Arrays.fill(this.slots, initialValue);
        this.offset = 0;
    }

    public void add(long value) {
        this.slots[this.offset] = value;
        this.offset = ++this.offset % this.slots.length;
    }

    public long average() {
        long sum = 0;

        for (long val : this.slots) {
            sum += val;
        }

        return sum / this.slots.length;
    }
}
