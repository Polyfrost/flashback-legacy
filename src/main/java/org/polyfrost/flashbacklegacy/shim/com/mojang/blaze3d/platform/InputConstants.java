package org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform;

import com.moulberry.flashback.utils.InputHelper;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLKeyboard;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.input.KeyEvent;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

public class InputConstants {
    public static final Key UNKNOWN = new Key(Type.KEYBOARD, -1);

    private static final int[] SCANCODE_TO_LEGACY = new int[512];
    private static final int[] LEGACY_TO_SCANCODE = new int[256];

    static {
        try {
            Method translate = Class.forName("pl.tomgirl.pylon.window.Keymap").getDeclaredMethod("translate", int.class);
            translate.setAccessible(true);
            for (int scancode = SCANCODE_TO_LEGACY.length - 1; scancode > 0; scancode--) {
                int legacy = Math.max(0, (int) translate.invoke(null, scancode));
                SCANCODE_TO_LEGACY[scancode] = legacy;
                if (legacy > 0 && legacy < LEGACY_TO_SCANCODE.length) LEGACY_TO_SCANCODE[legacy] = scancode;
            }
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Key fromLegacy(int code) {
        if (code < 0) return Type.MOUSE.getOrCreate(code + 100);
        if (code == 0 || code >= LEGACY_TO_SCANCODE.length || LEGACY_TO_SCANCODE[code] == 0) return UNKNOWN;
        return Type.KEYBOARD.getOrCreate(LEGACY_TO_SCANCODE[code]);
    }

    public static Key getKey(KeyEvent event) {
        return Type.KEYBOARD.getOrCreate(event.key());
    }

    public static void releaseMouse(Window window, double x, double y) {
        Minecraft.getInstance().unlockMouse();
    }

    public enum Type {
        KEYBOARD,
        SCANCODE,
        MOUSE;

        public Key getOrCreate(int value) {
            return new Key(this, value);
        }
    }

    public static final class Key {
        private final Type type;
        private final int value;

        Key(Type type, int value) {
            this.type = type;
            this.value = value;
        }

        public Type getType() {
            return type;
        }

        public int getValue() {
            return value;
        }

        public Component getDisplayName() {
            if (type == Type.MOUSE) return Component.literal("Mouse " + (value + 1));
            int scancode = value >= SCANCODE_TO_LEGACY.length ? InputHelper.imguiKeyToSdlScancode(value) : value;
            String name = scancode <= 0 ? null : SDLKeyboard.SDL_GetKeyName(SDLKeyboard.SDL_GetKeyFromScancode(scancode, (short) 0, false));
            return Component.literal(name == null || name.isEmpty() ? "?" : name);
        }

        public int toLegacy() {
            if (type == Type.MOUSE) return value - 100;
            return value > 0 && value < SCANCODE_TO_LEGACY.length ? SCANCODE_TO_LEGACY[value] : 0;
        }
    }
}
