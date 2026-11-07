package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.living.LivingEntity;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.text.Text;
import org.polyfrost.flashbacklegacy.editor.EntityOverrides;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V", at = @At("HEAD"), cancellable = true)
    private void flashback$nametag(LivingEntity entity, double dx, double dy, double dz, CallbackInfo ci) {
        if (EditorUi.hidesNametag(entity)) ci.cancel();
    }

    @Redirect(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;getFormattedString()Ljava/lang/String;"))
    private String flashback$nameOverride(Text name, LivingEntity entity) {
        return EntityOverrides.nameTag(entity, name.getFormattedString());
    }
}
