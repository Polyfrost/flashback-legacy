package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

import net.minecraft.client.gui.screen.Screen;

public class KeyboardHandler {
    public String getClipboard() {
        return Screen.getClipboard();
    }

    public void setClipboard(String text) {
        Screen.setClipboard(text);
    }
}
