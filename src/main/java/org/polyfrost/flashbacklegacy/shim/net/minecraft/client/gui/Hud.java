package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui;

public class Hud {
    public boolean isHidden() {
        return net.minecraft.client.Minecraft.getInstance().options.hideGui;
    }

    public void toggle() {
        var options = net.minecraft.client.Minecraft.getInstance().options;
        options.hideGui = !options.hideGui;
    }
}
