package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event.lifecycle.v1;

import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event.Event;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.server.MinecraftServer;

public final class ServerTickEvents {
    public static final Event<StartTick> START_SERVER_TICK = new Event<>(listeners -> server -> listeners.forEach(l -> l.onStartTick(server)));
    public static final Event<EndTick> END_SERVER_TICK = new Event<>(listeners -> server -> listeners.forEach(l -> l.onEndTick(server)));

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }
}
