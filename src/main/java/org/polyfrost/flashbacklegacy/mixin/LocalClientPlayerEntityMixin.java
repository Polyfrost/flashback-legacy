package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.SoundEventS2CPacket;
import org.polyfrost.flashbacklegacy.record.LegacyPacket;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalClientPlayerEntity.class)
public abstract class LocalClientPlayerEntityMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void flashback$clientCommand(String message, CallbackInfo ci) {
        if (message.startsWith("/flashback-legacy ")) {
            String[] args = message.substring(18).split(" ", 2);
            org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.SCHEDULED.add(() -> LegacyReplayServer.command(args));
            ci.cancel();
        } else if (message.startsWith("/") && ClientCommands.execute(message.substring(1))) ci.cancel();
    }

    @Inject(method = "playSound", at = @At("HEAD"))
    private void flashback$recordSound(String id, float volume, float pitch, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        LegacyPacket.record(new SoundEventS2CPacket(id, self.x, self.y, self.z, volume, pitch));
    }
}
