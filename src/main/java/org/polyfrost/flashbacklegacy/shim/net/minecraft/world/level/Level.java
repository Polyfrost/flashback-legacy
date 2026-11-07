package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.scores.Scoreboard;

public abstract class Level implements LevelAccessor, AutoCloseable {
    public abstract Scoreboard getScoreboard();

    public abstract Entity getEntity(int id);
}
