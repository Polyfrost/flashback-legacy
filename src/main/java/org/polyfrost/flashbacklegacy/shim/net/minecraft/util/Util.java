package org.polyfrost.flashbacklegacy.shim.net.minecraft.util;

public class Util {
    private static final OS PLATFORM = detect();

    public enum OS {
        LINUX,
        SOLARIS,
        WINDOWS,
        OSX,
        UNKNOWN
    }

    private static OS detect() {
        String name = System.getProperty("os.name").toLowerCase();
        if (name.contains("win")) return OS.WINDOWS;
        if (name.contains("mac")) return OS.OSX;
        if (name.contains("linux") || name.contains("unix")) return OS.LINUX;
        return OS.UNKNOWN;
    }

    public static OS getPlatform() {
        return PLATFORM;
    }

    public static org.polyfrost.flashbacklegacy.shim.net.minecraft.TracingExecutor backgroundExecutor() {
        return org.polyfrost.flashbacklegacy.shim.net.minecraft.TracingExecutor.BACKGROUND;
    }

    public static long getMillis() {
        return System.nanoTime() / 1_000_000L;
    }
}
