package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol.game;

import org.polyfrost.flashbacklegacy.record.LegacyPacket;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol.SimpleUnboundProtocol;

public class GameProtocols {
    private static final SimpleUnboundProtocol<?, ?> CLIENTBOUND_TEMPLATE = decorator -> () -> LegacyPacket.CODEC;

    public static SimpleUnboundProtocol<?, ?> get$CLIENTBOUND_TEMPLATE() {
        return CLIENTBOUND_TEMPLATE;
    }
}
