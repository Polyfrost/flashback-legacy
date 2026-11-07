package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.resources.language;

public class I18n {
    public static String get(String key, Object... args) {
        try {
            return net.minecraft.client.resource.language.I18n.translate(key, args);
        } catch (NullPointerException e) {
            return key;
        }
    }
}
