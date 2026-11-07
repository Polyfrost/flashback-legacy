package org.polyfrost.flashbacklegacy.mixin;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void flashback$recordedPlayerView(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || !(mc.getServer() instanceof LegacyReplayServer server)) return;
        Entity self = (Entity) (Object) this;
        cir.setReturnValue(self.world.getEntity(server.localPlayerId()) instanceof PlayerEntity recorded ? self.isInvisibleTo(recorded) : self.isInvisible());
    }

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/Random"))
    private Random flashback$exportRandom() {
        return EditorUi.entityRandom((Entity) (Object) this);
    }
}
