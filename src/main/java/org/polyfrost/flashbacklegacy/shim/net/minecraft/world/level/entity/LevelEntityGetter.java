package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.entity;

import java.util.UUID;

public interface LevelEntityGetter<T extends EntityAccess> {
    T get(UUID uuid);
}
