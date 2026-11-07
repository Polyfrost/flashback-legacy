package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity;

import net.minecraft.entity.living.player.PlayerEntity;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.PlayerModelPart;

public abstract class Avatar extends LivingEntity {
    protected Avatar(PlayerEntity handle) {
        super(handle);
    }

    public boolean isModelPartShown(PlayerModelPart part) {
        return (handle.getSyncedData().getByte(10) & 1 << part.ordinal()) != 0;
    }
}
