package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class ReplayServerWorldMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = {"tickChunks", "tickWeather"}, at = @At("HEAD"), cancellable = true)
    private void flashback$skip(CallbackInfo ci) {
        if (server instanceof LegacyReplayServer) ci.cancel();
    }

    @Inject(method = "doScheduledTicks", at = @At("HEAD"), cancellable = true)
    private void flashback$skipScheduled(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (server instanceof LegacyReplayServer) cir.setReturnValue(false);
    }
}
