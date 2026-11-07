package org.polyfrost.flashbacklegacy.shim.net.minecraft.util;

public class Mth {
    public static float wrapDegrees(float degrees) {
        float f = degrees % 360;
        if (f >= 180) f -= 360;
        if (f < -180) f += 360;
        return f;
    }

    public static double wrapDegrees(double degrees) {
        double d = degrees % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    public static double lerp(double delta, double from, double to) {
        return from + delta * (to - from);
    }

    public static float lerp(float delta, float from, float to) {
        return from + delta * (to - from);
    }

    public static float rotLerp(float delta, float from, float to) {
        return from + delta * wrapDegrees(to - from);
    }

    public static float sin(double value) {
        return (float) Math.sin(value);
    }

    public static float cos(double value) {
        return (float) Math.cos(value);
    }

    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public static int floor(float value) {
        return (int) Math.floor(value);
    }

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int smallestEncompassingPowerOfTwo(int value) {
        return value <= 1 ? 1 : Integer.highestOneBit(value - 1) << 1;
    }
}
