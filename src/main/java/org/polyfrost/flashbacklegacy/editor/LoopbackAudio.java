package org.polyfrost.flashbacklegacy.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sound.system.SoundEngine;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryStack;
import org.polyfrost.flashbacklegacy.mixin.SoundEngineAccessor;
import org.polyfrost.flashbacklegacy.mixin.SoundManagerAccessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.LockSupport;

public final class LoopbackAudio {
    private static volatile int channels;
    private static volatile long device;

    private LoopbackAudio() {
    }

    public static boolean enabled() {
        return channels != 0;
    }

    public static boolean isDevice(long dev) {
        return channels != 0 && dev == device;
    }

    public static long openDevice() {
        return device = SOFTLoopback.alcLoopbackOpenDeviceSOFT((CharSequence) null);
    }

    public static long createContext(long dev) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return ALC10.alcCreateContext(dev, stack.ints(
                SOFTLoopback.ALC_FORMAT_TYPE_SOFT, SOFTLoopback.ALC_FLOAT_SOFT,
                SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT, channels == 2 ? SOFTLoopback.ALC_STEREO_SOFT : SOFTLoopback.ALC_MONO_SOFT,
                ALC10.ALC_FREQUENCY, 48000, 0));
        }
    }

    public static void begin(boolean stereo) {
        channels = stereo ? 2 : 1;
        restart();
    }

    public static void end() {
        if (channels == 0) return;
        channels = 0;
        device = 0;
        restart();
    }

    private static void restart() {
        SoundEngine engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).getEngine();
        engine.reload();
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!((SoundEngineAccessor) engine).isStarted() && System.nanoTime() < deadline) LockSupport.parkNanos(1_000_000L);
    }

    public static FloatBuffer render(int samples) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(samples * 4 * Math.max(channels, 1)).order(ByteOrder.nativeOrder()).asFloatBuffer();
        if (device != 0) SOFTLoopback.alcRenderSamplesSOFT(device, buffer, samples);
        return buffer;
    }
}
