package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @Shadow
    public KeyBinding[] keyBindings;

    @Inject(method = "load", at = @At("HEAD"))
    private void flashback$addKeyMappings(CallbackInfo ci) {
        keyBindings = ArrayUtils.addAll(keyBindings, KeyMapping.PENDING.toArray(KeyBinding[]::new));
        KeyMapping.PENDING.clear();
    }
}
