package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

import net.minecraft.client.options.GameOptions;

import java.util.Arrays;

public class Options {
    private static Options instance;

    public final GameOptions handle;

    private Options(GameOptions handle) {
        this.handle = handle;
    }

    public static Options of(GameOptions handle) {
        if (handle == null) return null;
        if (instance == null || instance.handle != handle) instance = new Options(handle);
        return instance;
    }

    public OptionInstance<Boolean> improvedTransparency() {
        return new OptionInstance<>(() -> false, v -> {});
    }

    public CameraType getCameraType() {
        return CameraType.values()[handle.perspective];
    }

    public void save() {
        handle.save();
    }

    public KeyMapping[] get$keyMappings() {
        return Arrays.stream(handle.keyBindings).map(KeyMapping::of).toArray(KeyMapping[]::new);
    }

    public KeyMapping get$keyUp() {
        return KeyMapping.of(handle.forwardKey);
    }

    public KeyMapping get$keyLeft() {
        return KeyMapping.of(handle.leftKey);
    }

    public KeyMapping get$keyDown() {
        return KeyMapping.of(handle.backKey);
    }

    public KeyMapping get$keyRight() {
        return KeyMapping.of(handle.rightKey);
    }

    public KeyMapping get$keyJump() {
        return KeyMapping.of(handle.jumpKey);
    }

    public KeyMapping get$keyChat() {
        return KeyMapping.of(handle.chatKey);
    }

    public KeyMapping get$keyCommand() {
        return KeyMapping.of(handle.commandKey);
    }

    public KeyMapping get$keySprint() {
        return KeyMapping.of(handle.sprintKey);
    }
}
