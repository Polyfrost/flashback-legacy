package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.render.ItemInHandRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyVariable(method = "renderInFirstPerson", at = @At("STORE"), ordinal = 0)
    private ClientPlayerEntity flashback$spectatedHands(ClientPlayerEntity player) {
        ClientPlayerEntity spectated = LegacyReplayServer.spectatedPlayer();
        return spectated != null ? spectated : player;
    }

    @ModifyVariable(method = "renderInFirstPerson", at = @At(value = "LOAD", ordinal = 8), ordinal = 0)
    private ClientPlayerEntity flashback$viewerSway(ClientPlayerEntity player) {
        return Minecraft.getInstance().player;
    }

    @ModifyVariable(method = "renderInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/ItemInHandRenderer;applyHandSway(Lnet/minecraft/client/entity/living/player/LocalClientPlayerEntity;F)V", shift = At.Shift.AFTER), ordinal = 0)
    private ClientPlayerEntity flashback$afterSway(ClientPlayerEntity player) {
        return flashback$spectatedHands(player);
    }

    @ModifyArg(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;"))
    private Entity flashback$spectatedRenderer(Entity viewer, @Local(argsOnly = true) ClientPlayerEntity player) {
        return player;
    }

    @ModifyArg(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerRenderer;renderRightArm(Lnet/minecraft/client/entity/living/player/ClientPlayerEntity;)V"))
    private ClientPlayerEntity flashback$spectatedArm(ClientPlayerEntity viewer, @Local(argsOnly = true) ClientPlayerEntity player) {
        return player;
    }

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private PlayerEntity flashback$spectatedItem(PlayerEntity player) {
        ClientPlayerEntity spectated = LegacyReplayServer.spectatedPlayer();
        return spectated != null ? spectated : player;
    }
}
