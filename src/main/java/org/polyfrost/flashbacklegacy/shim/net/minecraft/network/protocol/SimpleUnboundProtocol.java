package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol;

import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.ProtocolInfo;

public interface SimpleUnboundProtocol<T, B extends ByteBuf> extends ProtocolInfo.DetailsProvider {
    ProtocolInfo<T> bind(Function<ByteBuf, B> decorator);
}
