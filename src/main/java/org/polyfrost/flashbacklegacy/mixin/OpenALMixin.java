package org.polyfrost.flashbacklegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.openal.AL;
import org.polyfrost.flashbacklegacy.editor.LoopbackAudio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.IntBuffer;

@Mixin(value = AL.class, remap = false)
public abstract class OpenALMixin {
    @WrapOperation(method = "init(Ljava/lang/String;IIZZ)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcOpenDevice(Ljava/lang/CharSequence;)J"))
    private static long flashback$openDevice(CharSequence name, Operation<Long> original) {
        return LoopbackAudio.enabled() ? LoopbackAudio.openDevice() : original.call(name);
    }

    @WrapOperation(method = "init(Ljava/lang/String;IIZZ)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcCreateContext(JLjava/nio/IntBuffer;)J"), require = 2)
    private static long flashback$createContext(long device, IntBuffer attributes, Operation<Long> original) {
        return LoopbackAudio.isDevice(device) ? LoopbackAudio.createContext(device) : original.call(device, attributes);
    }
}
