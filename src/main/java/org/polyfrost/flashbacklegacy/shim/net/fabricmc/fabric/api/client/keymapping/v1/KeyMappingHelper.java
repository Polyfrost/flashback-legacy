package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.keymapping.v1;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.KeyMapping;

public final class KeyMappingHelper {
    public static KeyMapping registerKeyMapping(KeyMapping mapping) {
        mapping.register();
        return mapping;
    }
}
