package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.input;

public record KeyEvent(int key, int scancode, int modifiers) implements InputWithModifiers {
    public boolean hasShiftDown() {
        return (modifiers & 1) != 0;
    }

    public boolean hasControlDownWithQuirk() {
        return (modifiers & (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY ? 8 : 2)) != 0;
    }

    public boolean isSelection() {
        return false;
    }
}
