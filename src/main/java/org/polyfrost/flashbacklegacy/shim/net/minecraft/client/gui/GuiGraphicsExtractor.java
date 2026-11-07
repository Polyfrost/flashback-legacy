package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.Window;

public class GuiGraphicsExtractor {
    public static final GuiGraphicsExtractor INSTANCE = new GuiGraphicsExtractor();

    public int guiWidth() {
        return new Window(Minecraft.getInstance()).getWidth();
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        GuiElement.fill(x0, y0, x1, y1, color);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        Minecraft.getInstance().textRenderer.draw(text, x, y, color, shadow);
    }
}
