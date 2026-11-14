package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.render.pipeline.RenderTarget;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
    @WrapOperation(method = "draw(IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;viewport(IIII)V"))
    private void flashback$frameViewport(int x, int y, int width, int height, Operation<Void> original) {
        int[] v = EditorUi.frameViewport;
        if (v != null) original.call(v[0], v[1], v[2], v[3]);
        else original.call(x, y, width, height);
    }
}
