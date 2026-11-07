package org.polyfrost.flashbacklegacy.mixin;

import java.util.List;

import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityAccessor {
    @Accessor
    List<Integer> getRemovedEntities();
}
