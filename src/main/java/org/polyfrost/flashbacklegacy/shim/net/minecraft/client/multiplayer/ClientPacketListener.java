package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;

import java.util.Collection;
import java.util.UUID;

public class ClientPacketListener extends ClientCommonPacketListenerImpl {
    public final ClientPlayNetworkHandler handle;

    public ClientPacketListener(ClientPlayNetworkHandler handle) {
        this.handle = handle;
    }

    public void sendCommand(String command) {
        if (command.startsWith("spectate ") && LegacyReplayServer.spectate(UUID.fromString(command.substring(9)))) return;
        Minecraft.getInstance().player.sendChat("/" + command);
    }

    public Collection<PlayerInfo> getOnlinePlayers() {
        return handle.getOnlinePlayers().stream().map(PlayerInfo::new).toList();
    }

    public Collection<PlayerInfo> getListedOnlinePlayers() {
        return getOnlinePlayers();
    }
}
