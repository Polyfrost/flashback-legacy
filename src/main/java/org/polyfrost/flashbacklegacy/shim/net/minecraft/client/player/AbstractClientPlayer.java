package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.player;

import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.scoreboard.team.Team;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.Player;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.PlayerSkin;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.scores.PlayerTeam;

public class AbstractClientPlayer extends Player {
    public AbstractClientPlayer(ClientPlayerEntity handle) {
        super(handle);
    }

    public PlayerSkin getSkin() {
        return new PlayerSkin(((ClientPlayerEntity) handle).getCapeTextureLocation() == null ? null : PlayerSkin.HAS_CAPE);
    }

    public String getScoreboardName() {
        return handle.getName();
    }

    public PlayerTeam getTeam() {
        return ((ClientPlayerEntity) handle).getScoreboardTeam() instanceof Team team ? new PlayerTeam(team) : null;
    }
}
