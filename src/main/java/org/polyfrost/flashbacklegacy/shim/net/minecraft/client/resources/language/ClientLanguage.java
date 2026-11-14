package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.resources.language;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.locale.Language;

import java.util.Map;

public class ClientLanguage extends Language {
    public Map<String, String> storage = Map.of();
}
