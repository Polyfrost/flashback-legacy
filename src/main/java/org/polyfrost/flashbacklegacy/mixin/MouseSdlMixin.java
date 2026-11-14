package org.polyfrost.flashbacklegacy.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.tomgirl.pylon.window.MouseSdl;

@Mixin(value = MouseSdl.class, remap = false)
public abstract class MouseSdlMixin {
    @ModifyExpressionValue(method = "toLwjglY", at = @At(value = "INVOKE", target = "Lpl/tomgirl/pylon/window/DisplaySdl;getHeight()I"))
    private int flashback$frameHeight(int height) {
        return EditorUi.mouseFlipHeight(height);
    }
}
