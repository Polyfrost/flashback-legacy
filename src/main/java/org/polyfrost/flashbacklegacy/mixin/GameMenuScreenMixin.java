package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.screen.RecordingIconButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    @Unique
    private static final int START = 0x46424C, FINISH = START + 1, PAUSE = START + 2, UNPAUSE = START + 3, CANCEL = START + 4;

    @Unique
    private int flashback$titleY = -1;

    @Inject(method = "init", at = @At("TAIL"))
    private void flashback$addControls(CallbackInfo ci) {
        flashback$titleY = -1;
        if (!FlashbackLegacy.flashbackLoaded || Flashback.isInReplay()) return;
        switch (Flashback.getConfig().recordingControls.controlsLocation) {
            case RIGHT -> flashback$addSide(width / 2 + 104);
            case LEFT -> flashback$addSide(width / 2 - 124);
            case BELOW -> flashback$addBelow();
            case HIDDEN -> {}
        }
    }

    @Unique
    private void flashback$addSide(int x) {
        int y = height / 4 + 32;
        if (Flashback.RECORDER == null) {
            buttons.add(new RecordingIconButton(START, x, y, I18n.translate("flashback.recording_controls.start"), "start"));
            return;
        }
        boolean paused = Flashback.RECORDER.isPaused();
        buttons.add(new RecordingIconButton(FINISH, x, y, I18n.translate("flashback.recording_controls.finish"), "finish"));
        buttons.add(new RecordingIconButton(paused ? UNPAUSE : PAUSE, x, y + 24, I18n.translate(paused ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause"), paused ? "start" : "pause"));
        buttons.add(new RecordingIconButton(CANCEL, x, y + 48, I18n.translate("flashback.recording_controls.cancel"), "cancel"));
    }

    @Unique
    private void flashback$addBelow() {
        int shift = Math.max(0, height / 4 + 188 - height);
        for (ButtonWidget button : buttons) button.y -= shift;
        flashback$titleY = height / 4 + 130 - shift;
        int x = width / 2 - 100, y = flashback$titleY + 12;
        if (Flashback.RECORDER == null) {
            buttons.add(new ButtonWidget(START, x, y, I18n.translate("flashback.recording_controls.start")));
            return;
        }
        boolean paused = Flashback.RECORDER.isPaused();
        buttons.add(new ButtonWidget(FINISH, x, y, I18n.translate("flashback.recording_controls.finish")));
        buttons.add(new ButtonWidget(paused ? UNPAUSE : PAUSE, x, y + 24, 98, 20, I18n.translate(paused ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause")));
        buttons.add(new ButtonWidget(CANCEL, x + 102, y + 24, 98, 20, I18n.translate("flashback.recording_controls.cancel")));
    }

    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    private void flashback$clicked(ButtonWidget button, CallbackInfo ci) {
        if (button.id < START || button.id > CANCEL) return;
        ci.cancel();
        switch (button.id) {
            case START -> Flashback.startRecordingReplay();
            case FINISH -> Flashback.finishRecordingReplay();
            case PAUSE -> Flashback.pauseRecordingReplay(true);
            case UNPAUSE -> Flashback.pauseRecordingReplay(false);
            case CANCEL -> {
                minecraft.openScreen(new ConfirmScreen(this, I18n.translate("flashback.confirm_cancel_recording"), I18n.translate("flashback.confirm_cancel_recording_description"), 0));
                return;
            }
        }
        minecraft.openScreen(null);
    }

    @Override
    public void confirmResult(boolean confirmed, int id) {
        if (confirmed) Flashback.cancelRecordingReplay();
        minecraft.openScreen(confirmed ? null : this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void flashback$renderExtras(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        if (flashback$titleY >= 0) drawCenteredString(textRenderer, I18n.translate("flashback.flashback"), width / 2, flashback$titleY, 0xFFFFFF);
        for (ButtonWidget button : buttons) {
            if (button instanceof RecordingIconButton icon && icon.isHovered()) renderTooltip(icon.label, mouseX, mouseY);
        }
    }
}
