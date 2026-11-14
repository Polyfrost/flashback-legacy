package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.rendering.v1.hud;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public interface HudElementRegistry {
    Map<Identifier, HudElement> ELEMENTS = new LinkedHashMap<>();

    static void addLast(Identifier id, HudElement element) {
        ELEMENTS.put(id, element);
    }
}
