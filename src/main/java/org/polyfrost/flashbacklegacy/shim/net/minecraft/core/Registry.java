package org.polyfrost.flashbacklegacy.shim.net.minecraft.core;

import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Keyable;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.ResourceKey;

public interface Registry<T> extends IdMap, Keyable, HolderLookup.RegistryLookup {
    Set<Identifier> keySet();

    T getValue(Identifier id);

    Set<Map.Entry<ResourceKey, T>> entrySet();

    default Identifier getKey(T value) {
        return null;
    }
}
