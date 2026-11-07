package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.networking.v1.context;

public interface PacketContext {
    static PacketContext get() {
        return null;
    }

    static void runWithContext(PacketContextProvider provider, Runnable runnable) {
        runnable.run();
    }
}
