package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui;

import net.minecraft.client.Minecraft;

public class Hud {
    public boolean isHidden() {
        return Minecraft.getInstance().options.hideGui;
    }

    public void toggle() {
        var options = Minecraft.getInstance().options;
        options.hideGui = !options.hideGui;
    }
}
