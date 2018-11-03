package com.borunovv.core.util;

public final class LongRange {

    public final long min;
    public final long max;

    public LongRange(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public boolean contains(long value) {
        return min <= value && value <= max;
    }

    @Override
    public String toString() {
        return "LongRange{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
