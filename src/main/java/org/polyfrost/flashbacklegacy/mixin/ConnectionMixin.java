package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.record.Recorder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.NetworkProtocol;
import net.minecraft.network.PacketFlow;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.record.LegacyPacket;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.ConnectionProtocol;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    @Final
    private PacketFlow flow;

    @Shadow
    private Channel channel;

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void flashback$record(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof KeepAliveS2CPacket keepAlive && keepAlive.getTimeMillis() == LegacyReplayServer.SYNC_ID) {
            LegacyReplayServer.serverSynced = true;
            ci.cancel();
            return;
        }
        if (packet instanceof KeepAliveC2SPacket keepAlive && keepAlive.getTimeMillis() == LegacyReplayServer.SYNC_ID) {
            LegacyReplayServer.clientSynced = true;
            ci.cancel();
            return;
        }
        if (flow != PacketFlow.CLIENTBOUND || !FlashbackLegacy.flashbackLoaded || packet instanceof KeepAliveS2CPacket) return;
        Recorder recorder = Flashback.RECORDER;
        if (recorder != null && channel.attr(Connection.PROTOCOL).get() == NetworkProtocol.PLAY) {
            recorder.writePacketAsync(LegacyPacket.of(packet), ConnectionProtocol.PLAY);
        }
    }
}
