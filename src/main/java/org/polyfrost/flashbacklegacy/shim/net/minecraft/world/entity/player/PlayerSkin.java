package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.ClientAsset;

public record PlayerSkin(ClientAsset.Texture cape) {
    public static final ClientAsset.Texture HAS_CAPE = new ClientAsset.Texture() {
    };
}
