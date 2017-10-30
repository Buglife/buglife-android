package com.buglife.sdk;

public class MathUtils {
    private MathUtils() {}

    public static int closest(int min, int max, int value) {
        int minDelta = Math.abs(value - min);
        int maxDelta = Math.abs(value - max);
        return minDelta < maxDelta ? min : max;
    }
}
