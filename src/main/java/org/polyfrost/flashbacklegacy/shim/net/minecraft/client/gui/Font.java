package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui;

public class Font {
    public static final Font INSTANCE = new Font();

    public int width(String text) {
        return net.minecraft.client.Minecraft.getInstance().textRenderer.getWidth(text);
    }
}
