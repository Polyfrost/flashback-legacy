package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform.InputConstants;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.input.KeyEvent;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

public class KeyMapping implements Comparable<KeyMapping> {
    public static final List<KeyBinding> PENDING = new ArrayList<>();

    private static final Map<KeyBinding, KeyMapping> WRAPPERS = new HashMap<>();

    private final KeyBinding binding;

    public KeyMapping(String name, InputConstants.Type type, int key, Category category) {
        this(new KeyBinding(name, type.getOrCreate(key).toLegacy(), category.translationKey()));
    }

    private KeyMapping(KeyBinding binding) {
        this.binding = binding;
        WRAPPERS.put(binding, this);
    }

    public static KeyMapping of(KeyBinding binding) {
        KeyMapping mapping = WRAPPERS.get(binding);
        return mapping != null ? mapping : new KeyMapping(binding);
    }

    public void register() {
        var options = net.minecraft.client.Minecraft.getInstance().options;
        if (options == null) PENDING.add(binding);
        else options.keyBindings = ArrayUtils.add(options.keyBindings, binding);
    }

    public boolean consumeClick() {
        return binding.consumeClick();
    }

    public boolean isDown() {
        return binding.isPressed();
    }

    public boolean isUnbound() {
        return binding.getKeyCode() == 0;
    }

    public InputConstants.Key get$key() {
        return InputConstants.fromLegacy(binding.getKeyCode());
    }

    public void setKey(InputConstants.Key key) {
        binding.setKeyCode(key.toLegacy());
    }

    public static void resetMapping() {
        KeyBinding.resetMapping();
    }

    public boolean matches(KeyEvent event) {
        InputConstants.Key key = get$key();
        return key.getType() == InputConstants.Type.KEYBOARD && key.getValue() == event.key() && !isUnbound();
    }

    public boolean same(KeyMapping other) {
        return binding.getKeyCode() == other.binding.getKeyCode();
    }

    public Component getTranslatedKeyMessage() {
        return Component.literal(GameOptions.getKeyName(binding.getKeyCode()));
    }

    public String getName() {
        return binding.getName();
    }

    @Override
    public int compareTo(KeyMapping o) {
        return binding.compareTo(o.binding);
    }

    public record Category(Identifier id) {
        public static Category register(Identifier id) {
            return new Category(id);
        }

        String translationKey() {
            return "key.category." + id.getNamespace() + "." + id.getPath();
        }
    }
}
