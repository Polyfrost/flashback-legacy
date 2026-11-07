package org.polyfrost.flashbacklegacy.mixin.flashback;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.moulberry.flashback.utils.AsyncFileDialogs;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallbackI;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.tomgirl.pylon.window.MainThread;

@Mixin(value = AsyncFileDialogs.class, remap = false)
public class AsyncFileDialogsMixin {
    @Redirect(method = "saveFileDialog", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLDialog;SDL_ShowSaveFileDialog(Lorg/lwjgl/sdl/SDL_DialogFileCallbackI;JJLorg/lwjgl/sdl/SDL_DialogFileFilter$Buffer;Ljava/lang/CharSequence;)V"))
    private static void flashback$save(SDL_DialogFileCallbackI callback, long userdata, long window, SDL_DialogFileFilter.Buffer filters, CharSequence location) {
        MainThread.run(() -> SDLDialog.SDL_ShowSaveFileDialog(callback, userdata, window, filters, location));
    }

    @Redirect(method = "openFileDialog", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLDialog;SDL_ShowOpenFileDialog(Lorg/lwjgl/sdl/SDL_DialogFileCallbackI;JJLorg/lwjgl/sdl/SDL_DialogFileFilter$Buffer;Ljava/lang/CharSequence;Z)V"))
    private static void flashback$open(SDL_DialogFileCallbackI callback, long userdata, long window, SDL_DialogFileFilter.Buffer filters, CharSequence location, boolean many) {
        MainThread.run(() -> SDLDialog.SDL_ShowOpenFileDialog(callback, userdata, window, filters, location, many));
    }

    @Redirect(method = "openFolderDialog", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLDialog;SDL_ShowOpenFolderDialog(Lorg/lwjgl/sdl/SDL_DialogFileCallbackI;JJLjava/lang/CharSequence;Z)V"))
    private static void flashback$folder(SDL_DialogFileCallbackI callback, long userdata, long window, CharSequence location, boolean many) {
        MainThread.run(() -> SDLDialog.SDL_ShowOpenFolderDialog(callback, userdata, window, location, many));
    }

    @ModifyReturnValue(method = {"saveFileDialog", "openFileDialog", "openFolderDialog"}, at = @At("RETURN"))
    private static CompletableFuture<String> flashback$completeOnClientThread(CompletableFuture<String> result) {
        return result.thenApplyAsync(Function.identity(), Minecraft.SCHEDULED::add);
    }
}
