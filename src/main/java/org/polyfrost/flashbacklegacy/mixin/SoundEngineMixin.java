package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.sound.FlashbackAudioManager;
import net.minecraft.client.sound.system.SoundEngine;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    private boolean started;

    @Inject(method = "close", at = @At("HEAD"))
    private void flashback$close(CallbackInfo ci) {
        if (!started || !FlashbackLegacy.flashbackLoaded) return;
        SoundManager.INSTANCE.soundEngine.channelAccess.clear();
        FlashbackAudioManager.invalidateLoadedBuffers();
    }
}
