package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.debug;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DebugScreenEntries {
    public static final Map<Identifier, DebugScreenEntry> ENTRIES = new LinkedHashMap<>();
    public static Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> PROFILES = new EnumMap<>(Map.of(
            DebugScreenProfile.DEFAULT, Map.of(),
            DebugScreenProfile.PERFORMANCE, Map.of()));

    public static Identifier register(Identifier id, DebugScreenEntry entry) {
        ENTRIES.put(id, entry);
        return id;
    }
}
