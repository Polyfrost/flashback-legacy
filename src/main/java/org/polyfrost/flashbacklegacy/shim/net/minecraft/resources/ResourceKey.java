package org.polyfrost.flashbacklegacy.shim.net.minecraft.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.Registry;

public final class ResourceKey<T> {
    private static final Map<String, ResourceKey<?>> VALUES = new ConcurrentHashMap<>();

    private final Identifier registry;
    private final Identifier identifier;

    private ResourceKey(Identifier registry, Identifier identifier) {
        this.registry = registry;
        this.identifier = identifier;
    }

    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<T> create(ResourceKey<? extends Registry<T>> registryKey, Identifier identifier) {
        Identifier registry = registryKey.identifier;
        return (ResourceKey<T>) VALUES.computeIfAbsent(registry + "/" + identifier, k -> new ResourceKey<>(registry, identifier));
    }

    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<Registry<T>> createRegistryKey(Identifier identifier) {
        Identifier root = Identifier.withDefaultNamespace("root");
        return (ResourceKey<Registry<T>>) VALUES.computeIfAbsent(root + "/" + identifier, k -> new ResourceKey<>(root, identifier));
    }

    public Identifier identifier() {
        return identifier;
    }

    public Identifier registry() {
        return registry;
    }

    @Override
    public String toString() {
        return "ResourceKey[" + registry + " / " + identifier + "]";
    }
}
