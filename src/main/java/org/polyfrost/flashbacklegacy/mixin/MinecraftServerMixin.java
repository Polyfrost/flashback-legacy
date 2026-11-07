package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.server.MinecraftServer;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L))
    private long flashback$tickMillis(long millis) {
        return (Object) this instanceof LegacyReplayServer server ? server.tickMillis() : millis;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void flashback$startTick(CallbackInfo ci) {
        ServerTickEvents.START_SERVER_TICK.invoker().onStartTick(null);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void flashback$endTick(CallbackInfo ci) {
        ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(null);
    }
}
