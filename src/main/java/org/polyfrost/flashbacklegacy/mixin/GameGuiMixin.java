package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.gui.chat.ChatGui;
import net.minecraft.client.gui.spectator.SpectatorGui;
import net.minecraft.client.network.PlayerInfo;
import net.minecraft.client.render.Window;
import net.minecraft.world.WorldSettings;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public abstract class GameGuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private int overlayMessageCooldown;
    @Shadow
    private int titleTime;

    @Inject(method = "render", at = @At("HEAD"))
    private void flashback$spectatedHud(float tickDelta, CallbackInfo ci) {
        ClientPlayerEntity spectated = LegacyReplayServer.spectatedPlayer();
        if (spectated != null) ((LegacyReplayServer) minecraft.getServer()).applyHud(spectated, minecraft.player);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void flashback$hud(float tickDelta, CallbackInfo ci) {
        for (HudElement element : HudElementRegistry.ELEMENTS.values()) element.extractRenderState(GuiGraphicsExtractor.INSTANCE, null);
    }

    private WorldSettings.GameMode flashback$cameraMode() {
        ClientPlayerEntity spectated = LegacyReplayServer.spectatedPlayer();
        PlayerInfo info = spectated == null ? null : minecraft.getNetworkHandler().getOnlinePlayer(spectated.getUuid());
        return info == null ? null : info.getGameMode();
    }

    @ModifyExpressionValue(method = {"render", "hasCrosshair"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientPlayerInteractionManager;hidesGui()Z"))
    private boolean flashback$cameraSpectator(boolean hidesGui) {
        WorldSettings.GameMode mode = flashback$cameraMode();
        return mode != null ? mode == WorldSettings.GameMode.SPECTATOR : hidesGui;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientPlayerInteractionManager;hasStatusBars()Z"))
    private boolean flashback$cameraStatusBars(boolean hasStatusBars) {
        WorldSettings.GameMode mode = flashback$cameraMode();
        return mode != null ? mode.isSurvival() : hasStatusBars;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientPlayerInteractionManager;hasXpBar()Z"))
    private boolean flashback$cameraXpBar(boolean hasXpBar) {
        WorldSettings.GameMode mode = flashback$cameraMode();
        return mode != null ? mode.isSurvival() : hasXpBar;
    }

    @Inject(method = {"renderHotbar", "renderStatusBars", "renderJumpBar", "renderXpBar", "renderSelectedItemName"}, at = @At("HEAD"), cancellable = true)
    private void flashback$hideHotbar(CallbackInfo ci) {
        if (EditorUi.hidesHud(v -> v.showHotbar)) ci.cancel();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/spectator/SpectatorGui;renderHotbar(Lnet/minecraft/client/render/Window;F)V"))
    private boolean flashback$hideSpectatorHotbar(SpectatorGui gui, Window window, float tickDelta) {
        return !EditorUi.hidesHud(v -> v.showHotbar);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/spectator/SpectatorGui;renderTooltip(Lnet/minecraft/client/render/Window;)V"))
    private boolean flashback$hideSpectatorTooltip(SpectatorGui gui, Window window) {
        return !EditorUi.hidesHud(v -> v.showHotbar);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/chat/ChatGui;render(I)V"))
    private boolean flashback$hideChat(ChatGui chat, int ticks) {
        return !EditorUi.hidesHud(v -> v.showChat);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GameGui;overlayMessageCooldown:I", opcode = Opcodes.GETFIELD))
    private int flashback$hideActionBar(int cooldown) {
        return EditorUi.hidesHud(v -> v.showActionBar) ? 0 : cooldown;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GameGui;titleTime:I", opcode = Opcodes.GETFIELD))
    private int flashback$hideTitle(int time) {
        return EditorUi.hidesHud(v -> v.showTitleText) ? 0 : time;
    }

    @Inject(method = "renderScoreboardObjective", at = @At("HEAD"), cancellable = true)
    private void flashback$hideScoreboard(CallbackInfo ci) {
        if (EditorUi.hidesHud(v -> v.showScoreboard)) ci.cancel();
    }

    @Inject(method = "renderBossBars", at = @At("HEAD"), cancellable = true)
    private void flashback$hideBossBar(CallbackInfo ci) {
        if (EditorUi.hidesHud(v -> v.showBossBar)) ci.cancel();
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void flashback$noVignette(CallbackInfo ci) {
        if (FlashbackLegacy.flashbackLoaded && Flashback.isInReplay()) ci.cancel();
    }
}
