package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.networking.v1;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.codec.StreamCodec;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public interface PayloadTypeRegistry {
    Map<Identifier, CustomPacketPayload.TypeAndCodec<?, ?>> CLIENTBOUND_PLAY = new HashMap<>();
    PayloadTypeRegistry CLIENTBOUND_PLAY_REGISTRY = new PayloadTypeRegistry() {
    };

    static PayloadTypeRegistry clientboundPlay() {
        return CLIENTBOUND_PLAY_REGISTRY;
    }

    default <T extends CustomPacketPayload> CustomPacketPayload.TypeAndCodec<?, T> register(CustomPacketPayload.Type<T> type, StreamCodec<?, T> codec) {
        CustomPacketPayload.TypeAndCodec<?, T> entry = new CustomPacketPayload.TypeAndCodec<>(type, codec);
        CLIENTBOUND_PLAY.put(type.id(), entry);
        return entry;
    }
}
