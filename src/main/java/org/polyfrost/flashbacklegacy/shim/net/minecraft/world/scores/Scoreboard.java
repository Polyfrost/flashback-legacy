package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.scores;

public class Scoreboard {
    public final net.minecraft.scoreboard.Scoreboard handle;

    public Scoreboard(net.minecraft.scoreboard.Scoreboard handle) {
        this.handle = handle;
    }

    public Objective getDisplayObjective(DisplaySlot slot) {
        return handle.getDisplayObjective(slot.ordinal()) == null ? null : new Objective();
    }
}
