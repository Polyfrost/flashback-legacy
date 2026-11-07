package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.player;

import net.minecraft.client.entity.living.player.Input;

public class ClientInput {
    public final Input handle;

    public ClientInput(Input handle) {
        this.handle = handle;
    }

    public org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.Input get$keyPresses() {
        return new org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.Input(
                handle.movementForward > 0, handle.movementForward < 0, handle.movementSideways > 0, handle.movementSideways < 0, handle.jumping, handle.sneaking, false);
    }
}
