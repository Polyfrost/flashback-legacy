package org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.audio;

import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

public class SoundBuffer {
    private final ByteBuffer data;
    private final AudioFormat format;
    private int buffer;

    public SoundBuffer(ByteBuffer data, AudioFormat format) {
        this.data = data;
        this.format = format;
    }

    int alBuffer() {
        if (buffer == 0) {
            boolean stereo = format.getChannels() == 2;
            int alFormat = format.getSampleSizeInBits() == 8
                ? stereo ? AL10.AL_FORMAT_STEREO8 : AL10.AL_FORMAT_MONO8
                : stereo ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16;
            buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, alFormat, data, (int) format.getSampleRate());
        }
        return buffer;
    }

    public void discardAlBuffer() {
        if (buffer != 0) AL10.alDeleteBuffers(buffer);
        buffer = 0;
    }
}
