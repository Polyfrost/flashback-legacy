package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.ProtocolInfo;

import java.util.function.Function;

public interface SimpleUnboundProtocol<T, B extends ByteBuf> extends ProtocolInfo.DetailsProvider {
    ProtocolInfo<T> bind(Function<ByteBuf, B> decorator);
}
