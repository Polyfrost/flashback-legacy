package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.moulberry.flashback.editor.ui.ReplayUI;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ReplayUI.class, remap = false)
public abstract class ReplayUIMixin {
    @Inject(method = "getLookTarget", at = @At("HEAD"), cancellable = true)
    private static void flashback$lookTarget(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(EditorUi.lookTarget());
    }
}
