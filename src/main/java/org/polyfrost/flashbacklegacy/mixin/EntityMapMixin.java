package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.EntityMap;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityMap.class)
public abstract class EntityMapMixin {
    @ModifyVariable(method = "startTracking(Lnet/minecraft/entity/Entity;IIZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int flashback$everyTick(int interval, Entity entity) {
        return LegacyReplayServer.isReplayEntity(entity) ? 1 : interval;
    }
}
