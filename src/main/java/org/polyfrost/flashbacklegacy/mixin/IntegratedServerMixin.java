package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.server.integrated.IntegratedServer;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isPaused()Z"))
    private boolean flashback$pausedByMenu(boolean paused) {
        return paused && !LegacyReplayServer.worldHeld;
    }
}
