package org.polyfrost.flashbacklegacy.shim.net.minecraft;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.storage.DataVersion;

public class SharedConstants {
    private static final WorldVersion VERSION = () -> new DataVersion(0, "main");

    public static WorldVersion getCurrentVersion() {
        return VERSION;
    }

    public static int getProtocolVersion() {
        return 47;
    }
}
