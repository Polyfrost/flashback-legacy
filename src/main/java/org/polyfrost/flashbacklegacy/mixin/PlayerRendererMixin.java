package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.polyfrost.flashbacklegacy.editor.EntityOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Redirect(method = "renderNameTag(Lnet/minecraft/client/entity/living/player/ClientPlayerEntity;DDDLjava/lang/String;FD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;getDisplayObjective(I)Lnet/minecraft/scoreboard/ScoreboardObjective;"))
    private ScoreboardObjective flashback$hideBelowName(Scoreboard scoreboard, int slot, ClientPlayerEntity player) {
        return EntityOverrides.hidesBelowName(player) ? null : scoreboard.getDisplayObjective(slot);
    }
}
