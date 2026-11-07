package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.sounds;

public class SoundManager {
    public static final SoundManager INSTANCE = new SoundManager();

    public final SoundEngine soundEngine = new SoundEngine();
}
