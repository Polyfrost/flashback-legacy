package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol.configuration;

import org.polyfrost.flashbacklegacy.record.LegacyPacket;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.ProtocolInfo;

public class ConfigurationProtocols {
    private static final ProtocolInfo<?> CLIENTBOUND = () -> LegacyPacket.CODEC;

    public static ProtocolInfo<?> get$CLIENTBOUND() {
        return CLIENTBOUND;
    }
}
