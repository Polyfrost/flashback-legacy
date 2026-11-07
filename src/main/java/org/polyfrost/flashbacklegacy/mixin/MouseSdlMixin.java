package org.polyfrost.flashbacklegacy.mixin;

import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.tomgirl.pylon.window.DisplaySdl;
import pl.tomgirl.pylon.window.MouseSdl;

@Mixin(value = MouseSdl.class, remap = false)
public abstract class MouseSdlMixin {
    @Redirect(method = "toLwjglY", at = @At(value = "INVOKE", target = "Lpl/tomgirl/pylon/window/DisplaySdl;getHeight()I"))
    private int flashback$frameHeight(DisplaySdl display) {
        return EditorUi.mouseFlipHeight(display.getHeight());
    }
}
