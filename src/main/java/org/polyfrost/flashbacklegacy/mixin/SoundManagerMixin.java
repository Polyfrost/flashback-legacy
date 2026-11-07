package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.playback.ReplayServer;
import net.minecraft.client.sound.system.SoundManager;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Inject(method = {"play(Lnet/minecraft/client/sound/instance/SoundInstance;)V", "play(Lnet/minecraft/client/sound/instance/SoundInstance;I)V"}, at = @At("HEAD"), cancellable = true)
    private void flashback$play(CallbackInfo ci) {
        ReplayServer replay = FlashbackLegacy.flashbackLoaded ? Flashback.getReplayServer() : null;
        if (replay != null && Flashback.EXPORT_JOB == null && replay.replayPaused) ci.cancel();
    }
}
