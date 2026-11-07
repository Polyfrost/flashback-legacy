package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.networking.v1;

import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event.Event;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.networking.v1.PacketSender;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ClientPacketListener;

public final class ClientPlayConnectionEvents {
    public static final Event<Join> JOIN = new Event<>(listeners -> (handler, sender, client) -> listeners.forEach(l -> l.onPlayReady(handler, sender, client)));

    @FunctionalInterface
    public interface Join {
        void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client);
    }
}
