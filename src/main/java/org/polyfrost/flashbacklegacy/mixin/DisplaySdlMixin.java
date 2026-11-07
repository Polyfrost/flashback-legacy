package org.polyfrost.flashbacklegacy.mixin;

import org.lwjgl.sdl.SDL_Event;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.tomgirl.pylon.window.DisplaySdl;

@Mixin(value = DisplaySdl.class, remap = false)
public abstract class DisplaySdlMixin {
    @Redirect(method = "pollEvents", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDL_Event;type()I"))
    private int flashback$imguiEvents(SDL_Event event) {
        return EditorUi.eventType(event);
    }
}
