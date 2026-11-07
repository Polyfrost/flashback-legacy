package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.event.lifecycle.v1;

import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event.Event;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft;

public final class ClientTickEvents {
    public static final Event<StartTick> START_CLIENT_TICK = new Event<>(listeners -> mc -> listeners.forEach(l -> l.onStartTick(mc)));
    public static final Event<EndTick> END_CLIENT_TICK = new Event<>(listeners -> mc -> listeners.forEach(l -> l.onEndTick(mc)));

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(Minecraft client);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(Minecraft client);
    }
}
