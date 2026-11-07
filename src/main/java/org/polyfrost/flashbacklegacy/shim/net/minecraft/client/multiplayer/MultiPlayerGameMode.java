package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer;

import net.minecraft.client.ClientPlayerInteractionManager;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.GameType;

public class MultiPlayerGameMode {
    public final ClientPlayerInteractionManager handle;

    public MultiPlayerGameMode(ClientPlayerInteractionManager handle) {
        this.handle = handle;
    }

    public GameType getPlayerMode() {
        return GameType.of(handle.getGameMode());
    }

    public boolean isSpectator() {
        return handle.isSpectator();
    }
}
