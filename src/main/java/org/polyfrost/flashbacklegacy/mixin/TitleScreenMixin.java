package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.screen.LegacySelectReplayScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique
    private static final int REPLAYS_BUTTON = 0x46424C;

    @Inject(method = "init", at = @At("TAIL"))
    private void flashback$addReplaysButton(CallbackInfo ci) {
        if (FlashbackLegacy.flashbackLoaded) buttons.add(new ButtonWidget(REPLAYS_BUTTON, width / 2 + 104, height / 4 + 48, 80, 20, I18n.translate("flashback.select_replay")));
    }

    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    private void flashback$openReplays(ButtonWidget button, CallbackInfo ci) {
        if (button.id == REPLAYS_BUTTON) {
            minecraft.openScreen(new LegacySelectReplayScreen(this));
            ci.cancel();
        }
    }
}
