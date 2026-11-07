package org.polyfrost.flashbacklegacy.mixin;

import java.nio.IntBuffer;

import org.lwjgl.openal.AL;
import org.polyfrost.flashbacklegacy.editor.LoopbackAudio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AL.class, remap = false)
public abstract class OpenALMixin {
    @Redirect(method = "init(Ljava/lang/String;IIZZ)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcOpenDevice(Ljava/lang/CharSequence;)J"))
    private static long flashback$openDevice(CharSequence name) {
        return LoopbackAudio.openDevice(name);
    }

    @Redirect(method = "init(Ljava/lang/String;IIZZ)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcCreateContext(JLjava/nio/IntBuffer;)J"), require = 2)
    private static long flashback$createContext(long device, IntBuffer attributes) {
        return LoopbackAudio.createContext(device, attributes);
    }
}
