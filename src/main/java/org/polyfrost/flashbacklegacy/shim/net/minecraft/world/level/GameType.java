package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level;

import net.minecraft.world.WorldSettings;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.util.StringRepresentable;

public enum GameType implements StringRepresentable {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR;

    public static final GameType DEFAULT_MODE = SURVIVAL;

    public static GameType byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : DEFAULT_MODE;
    }

    public static GameType of(WorldSettings.GameMode mode) {
        return mode == null || mode == WorldSettings.GameMode.NOT_SET ? DEFAULT_MODE : valueOf(mode.name());
    }

    public int getId() {
        return ordinal();
    }
}
