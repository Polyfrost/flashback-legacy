package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.input;

public class InputQuirks {
    public static final boolean REPLACE_CTRL_KEY_WITH_CMD_KEY = System.getProperty("os.name").toLowerCase().contains("mac");
}
