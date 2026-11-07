package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isPaused()Z"))
    private boolean flashback$pausedByMenu(Minecraft minecraft) {
        return minecraft.isPaused() && !LegacyReplayServer.worldHeld;
    }
}
