package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer;

import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.GameProfile;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.GameType;

public class PlayerInfo {
    public final net.minecraft.client.network.PlayerInfo handle;

    public PlayerInfo(net.minecraft.client.network.PlayerInfo handle) {
        this.handle = handle;
    }

    public GameProfile getProfile() {
        return GameProfile.of(handle.getProfile());
    }

    public GameType getGameMode() {
        return GameType.of(handle.getGameMode());
    }
}
