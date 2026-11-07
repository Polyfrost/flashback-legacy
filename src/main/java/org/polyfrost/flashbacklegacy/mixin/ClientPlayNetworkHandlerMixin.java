package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.LoginS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeS2CPacket;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void flashback$join(LoginS2CPacket packet, CallbackInfo ci) {
        ClientPlayConnectionEvents.JOIN.invoker().onPlayReady(null, null, Minecraft.getInstance());
    }

    @Inject(method = "handleWorldTime", at = @At("HEAD"), cancellable = true)
    private void flashback$pinnedTime(WorldTimeS2CPacket packet, CallbackInfo ci) {
        if (EditorUi.resetRngExport()) ci.cancel();
    }
}
