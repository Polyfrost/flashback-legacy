package org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d;

import java.net.URI;
import java.nio.file.Path;

import org.lwjgl.sdl.SDLMisc;

public class Blaze3D {
    public static void openUri(URI uri) {
        SDLMisc.SDL_OpenURL(uri.toString());
    }

    public static void openPath(Path path) {
        openUri(path.toUri());
    }
}
