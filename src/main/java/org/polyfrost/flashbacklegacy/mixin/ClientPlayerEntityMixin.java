package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.resource.Identifier;
import org.polyfrost.flashbacklegacy.editor.EntityOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "getSkinTextureLocation()Lnet/minecraft/resource/Identifier;", at = @At("HEAD"), cancellable = true)
    private void flashback$skin(CallbackInfoReturnable<Identifier> cir) {
        EntityOverrides.Skin skin = EntityOverrides.skin((ClientPlayerEntity) (Object) this);
        if (skin != null && skin.skin() != null) cir.setReturnValue(skin.skin());
    }

    @Inject(method = "getCapeTextureLocation", at = @At("HEAD"), cancellable = true)
    private void flashback$cape(CallbackInfoReturnable<Identifier> cir) {
        EntityOverrides.Skin skin = EntityOverrides.skin((ClientPlayerEntity) (Object) this);
        if (skin != null && skin.model() != null) cir.setReturnValue(skin.cape());
    }

    @Inject(method = "getModelType", at = @At("HEAD"), cancellable = true)
    private void flashback$model(CallbackInfoReturnable<String> cir) {
        EntityOverrides.Skin skin = EntityOverrides.skin((ClientPlayerEntity) (Object) this);
        if (skin != null && skin.model() != null) cir.setReturnValue(skin.model());
    }
}
