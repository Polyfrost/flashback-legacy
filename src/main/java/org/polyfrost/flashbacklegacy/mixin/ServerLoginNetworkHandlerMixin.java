package org.polyfrost.flashbacklegacy.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.handler.ServerLoginNetworkHandler;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    @Final
    private MinecraftServer server;
    @Shadow
    private GameProfile profile;

    @Inject(method = "acceptLogin", at = @At("HEAD"))
    private void flashback$viewerUuid(CallbackInfo ci) {
        if (!(server instanceof LegacyReplayServer)) return;
        GameProfile viewer = new GameProfile(UUID.randomUUID(), profile.getName());
        viewer.getProperties().putAll(profile.getProperties());
        profile = viewer;
    }
}
