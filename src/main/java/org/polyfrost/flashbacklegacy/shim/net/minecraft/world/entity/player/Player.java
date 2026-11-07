package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player;

import net.minecraft.entity.living.player.PlayerEntity;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Avatar;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.GameType;

public class Player extends Avatar {
    public Player(PlayerEntity handle) {
        super(handle);
    }

    public Abilities getAbilities() {
        return new Abilities(((PlayerEntity) handle).abilities);
    }

    public GameType gameMode() {
        return ((PlayerEntity) handle).isSpectator() ? GameType.SPECTATOR : GameType.SURVIVAL;
    }
}
