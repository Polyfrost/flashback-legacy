package org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform;

import com.moulberry.flashback.ext.WindowExt;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import pl.tomgirl.pylon.window.DisplaySdl;

public class Window implements AutoCloseable, WindowExt {
    public static final Window INSTANCE = new Window();

    public record FramebufferSize(int width, int height) {
    }

    public long handle() {
        return DisplaySdl.instance().getHandle();
    }

    public int getWidth() {
        return Minecraft.getInstance().width;
    }

    public int getHeight() {
        return Minecraft.getInstance().height;
    }

    public int getScreenWidth() {
        return Math.round((float) getWidth() * DisplaySdl.instance().getWindowWidth() / Math.max(1, Display.getWidth()));
    }

    public int getScreenHeight() {
        return Math.round((float) getHeight() * DisplaySdl.instance().getWindowHeight() / Math.max(1, Display.getHeight()));
    }

    public int get$framebufferWidth() {
        return getWidth();
    }

    public int get$framebufferHeight() {
        return getHeight();
    }

    public FramebufferSize queryFramebufferSize() {
        return new FramebufferSize(getWidth(), getHeight());
    }

    public boolean isExclusiveFullscreen() {
        return false;
    }

    public boolean isIconified() {
        return false;
    }

    @Override
    public void flashback$updateScaledFramebuffer(boolean resize) {
        EditorUi.updateGameSize();
    }

    @Override
    public void close() {
    }
}
