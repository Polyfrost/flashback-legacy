package org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.audio;

import org.lwjgl.openal.AL10;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys.Vec3;

public class Channel {
    public int source;

    public Channel(int source) {
        this.source = source;
    }

    private int state() {
        return AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
    }

    public boolean stopped() {
        return source == 0 || state() == AL10.AL_STOPPED;
    }

    public void stop() {
        if (source == 0) return;
        AL10.alDeleteSources(source);
        source = 0;
    }

    public void play() {
        AL10.alSourcePlay(source);
    }

    public void pause() {
        if (state() == AL10.AL_PLAYING) AL10.alSourcePause(source);
    }

    public void unpause() {
        if (state() == AL10.AL_PAUSED) AL10.alSourcePlay(source);
    }

    public void setPitch(float pitch) {
        AL10.alSourcef(source, AL10.AL_PITCH, pitch);
    }

    public void setVolume(float volume) {
        AL10.alSourcef(source, AL10.AL_GAIN, volume);
    }

    public void disableAttenuation() {
        AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0);
    }

    public void setLooping(boolean looping) {
        AL10.alSourcei(source, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    public void setSelfPosition(Vec3 pos) {
        AL10.alSource3f(source, AL10.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
    }

    public void setRelative(boolean relative) {
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    public void attachStaticBuffer(SoundBuffer buffer) {
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer.alBuffer());
    }
}
