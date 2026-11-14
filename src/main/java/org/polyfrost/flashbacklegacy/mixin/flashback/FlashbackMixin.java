package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.menu.TitleScreen;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(value = Flashback.class, remap = false)
public abstract class FlashbackMixin {
    @Inject(method = "openReplayWorld", at = @At("HEAD"), cancellable = true)
    private static void flashback$openReplayWorld(Path path, CallbackInfo ci) {
        LegacyReplayServer.play(path);
        ci.cancel();
    }

    @Inject(method = "canReplaceScreen", at = @At("HEAD"), cancellable = true)
    private static void flashback$canReplaceTitle(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof TitleScreen) cir.setReturnValue(true);
    }
}
