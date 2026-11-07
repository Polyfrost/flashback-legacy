package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.scores;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

public class PlayerTeam extends Team {
    public final net.minecraft.scoreboard.team.Team handle;

    public PlayerTeam(net.minecraft.scoreboard.team.Team handle) {
        this.handle = handle;
    }

    public Component getPlayerPrefix() {
        return Component.literal(handle.getPrefix());
    }

    public Component getPlayerSuffix() {
        return Component.literal(handle.getSuffix());
    }
}
