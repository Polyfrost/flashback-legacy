package org.polyfrost.flashbacklegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.sdl.SDL_Event;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.tomgirl.pylon.window.DisplaySdl;

@Mixin(value = DisplaySdl.class, remap = false)
public abstract class DisplaySdlMixin {
    @WrapOperation(method = "pollEvents", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDL_Event;type()I"))
    private int flashback$imguiEvents(SDL_Event event, Operation<Integer> original) {
        return EditorUi.eventType(event, original.call(event));
    }
}
