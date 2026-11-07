package org.polyfrost.flashbacklegacy.mixin;

import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor
    Random getRandom();

    @Accessor
    void setUuid(UUID uuid);
}
