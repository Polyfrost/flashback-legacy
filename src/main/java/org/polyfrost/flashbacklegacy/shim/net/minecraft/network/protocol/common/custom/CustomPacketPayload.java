package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol.common.custom;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.codec.StreamCodec;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(Identifier id) {
    }

    record TypeAndCodec<B, T extends CustomPacketPayload>(Type<T> type, StreamCodec<B, T> codec) {
    }
}
