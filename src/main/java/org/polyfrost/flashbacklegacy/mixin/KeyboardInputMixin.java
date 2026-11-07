package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.KeyboardInput;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void flashback$ignoreDuringExport(CallbackInfo ci) {
        if (EditorUi.exportSettings() == null) return;
        Input input = (Input) (Object) this;
        input.movementForward = input.movementSideways = 0;
        input.jumping = input.sneaking = false;
    }
}
