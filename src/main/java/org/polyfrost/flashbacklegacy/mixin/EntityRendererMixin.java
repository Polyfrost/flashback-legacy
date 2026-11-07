package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.visuals.ReplayVisuals;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "renderNameTag(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V", at = @At("HEAD"), cancellable = true)
    private void flashback$nametag(Entity entity, String name, double dx, double dy, double dz, int distance, CallbackInfo ci) {
        if (EditorUi.hidesNametag(entity)) ci.cancel();
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private void flashback$shadow(CallbackInfo ci) {
        ReplayVisuals visuals = EditorUi.visuals();
        if (visuals != null && !visuals.renderBlocks) ci.cancel();
    }
}
