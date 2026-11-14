package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui;

import net.minecraft.client.Minecraft;

public class Font {
    public static final Font INSTANCE = new Font();

    public int width(String text) {
        return Minecraft.getInstance().textRenderer.getWidth(text);
    }
}
