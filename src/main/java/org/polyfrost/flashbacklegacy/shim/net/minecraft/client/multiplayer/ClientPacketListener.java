package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer;

import java.util.Collection;

import net.minecraft.client.network.handler.ClientPlayNetworkHandler;

public class ClientPacketListener extends ClientCommonPacketListenerImpl {
    public final ClientPlayNetworkHandler handle;

    public ClientPacketListener(ClientPlayNetworkHandler handle) {
        this.handle = handle;
    }

    public void sendCommand(String command) {
        if (command.startsWith("spectate ") && org.polyfrost.flashbacklegacy.playback.LegacyReplayServer.spectate(java.util.UUID.fromString(command.substring(9)))) return;
        net.minecraft.client.Minecraft.getInstance().player.sendChat("/" + command);
    }

    public Collection<PlayerInfo> getOnlinePlayers() {
        return handle.getOnlinePlayers().stream().map(PlayerInfo::new).toList();
    }

    public Collection<PlayerInfo> getListedOnlinePlayers() {
        return getOnlinePlayers();
    }
}
