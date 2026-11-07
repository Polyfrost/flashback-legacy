package org.polyfrost.flashbacklegacy.shim.net.minecraft.network;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.codec.StreamCodec;

public interface ProtocolInfo<T> {
    StreamCodec<?, ?> codec();

    interface DetailsProvider {
    }
}
