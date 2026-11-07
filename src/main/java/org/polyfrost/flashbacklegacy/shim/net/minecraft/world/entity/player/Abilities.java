package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player;

import net.minecraft.entity.living.player.PlayerAbilities;

public class Abilities {
    public final PlayerAbilities handle;

    public Abilities(PlayerAbilities handle) {
        this.handle = handle;
    }

    public float getFlyingSpeed() {
        return handle.getFlySpeed();
    }

    public void setFlyingSpeed(float speed) {
        handle.setFlySpeed(speed);
    }

    public void set$flying(boolean flying) {
        handle.flying = flying;
    }
}
