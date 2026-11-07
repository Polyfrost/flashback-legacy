package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.render.platform.GlStateManager;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
    @Redirect(method = "draw(IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;viewport(IIII)V"))
    private void flashback$frameViewport(int x, int y, int width, int height) {
        int[] v = EditorUi.frameViewport;
        if (v != null) GlStateManager.viewport(v[0], v[1], v[2], v[3]);
        else GlStateManager.viewport(x, y, width, height);
    }
}
