package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.player;

import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.RegistryAccess;

public class LocalPlayer extends AbstractClientPlayer {
    public LocalPlayer(LocalClientPlayerEntity handle) {
        super(handle);
    }

    public org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ClientPacketListener get$connection() {
        return new org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ClientPacketListener(((LocalClientPlayerEntity) handle).networkHandler);
    }

    public RegistryAccess registryAccess() {
        return null;
    }

    public ClientInput get$input() {
        return new ClientInput(((LocalClientPlayerEntity) handle).input);
    }

    public void onUpdateAbilities() {
        ((LocalClientPlayerEntity) handle).syncAbilities();
    }
}
