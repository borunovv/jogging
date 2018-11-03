package com.borunovv.core.util;

public class IntRange {
    public final int min;
    public final int max; // inclusive!

    public IntRange(int min, int max) {
        Assert.isTrue(min <= max,
                "Expected min <= max, min=" + min + ", max=" + max);
        this.min = min;
        this.max = max;
    }

    public boolean contains(int value) {
        return min <= value && value <= max;
    }

    @Override
    public String toString() {
        return "IntRange [" + min + ", " + max + ']';
    }
}