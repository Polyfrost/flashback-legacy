package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.networking.v1;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class ClientPlayNetworking {
    public static final Map<Identifier, PlayPayloadHandler<?>> RECEIVERS = new HashMap<>();

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        return RECEIVERS.putIfAbsent(type.id(), handler) == null;
    }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        Minecraft client();
    }
}
