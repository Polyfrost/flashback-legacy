package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

public class MouseHandler {
    public void grabMouse() {
        net.minecraft.client.Minecraft.getInstance().lockMouse();
    }

    public void releaseMouse() {
        net.minecraft.client.Minecraft.getInstance().unlockMouse();
    }

    public void setIgnoreFirstMove() {
    }

    public void handleAccumulatedMovement() {
    }
}
