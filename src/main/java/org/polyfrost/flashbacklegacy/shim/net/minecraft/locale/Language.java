package org.polyfrost.flashbacklegacy.shim.net.minecraft.locale;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.resources.language.ClientLanguage;

public abstract class Language {
    private static final Language INSTANCE = new ClientLanguage();

    public static Language getInstance() {
        return INSTANCE;
    }
}
