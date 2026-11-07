package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer;

import net.minecraft.client.options.ServerListEntry;

public class ServerData {
    public final ServerListEntry handle;

    public ServerData(ServerListEntry handle) {
        this.handle = handle;
    }

    public String get$name() {
        return handle.name;
    }
}
