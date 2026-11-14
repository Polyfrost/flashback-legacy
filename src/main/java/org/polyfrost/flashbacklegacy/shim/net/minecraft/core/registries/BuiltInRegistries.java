package org.polyfrost.flashbacklegacy.shim.net.minecraft.core.registries;

import com.mojang.serialization.DynamicOps;
import net.minecraft.entity.Entities;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.DefaultedRegistry;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.Registry;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.particles.ParticleType;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.ResourceKey;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class BuiltInRegistries {
    private static final Registry<Registry<?>> REGISTRY = new MapRegistry<>(Map.of());
    private static DefaultedRegistry<EntityType<?>> entityTypes;
    private static Registry<ParticleType<?>> particleTypes;

    public static Registry<Registry<?>> get$REGISTRY() {
        return REGISTRY;
    }

    public static synchronized DefaultedRegistry<EntityType<?>> get$ENTITY_TYPE() {
        if (entityTypes == null) {
            Map<Identifier, EntityType<?>> entries = new LinkedHashMap<>();
            for (String key : Entities.getKeys()) entries.put(EntityType.id(key), new EntityType<>(key));
            entityTypes = new MapRegistry<>(entries);
        }
        return entityTypes;
    }

    public static synchronized Registry<ParticleType<?>> get$PARTICLE_TYPE() {
        if (particleTypes == null) {
            Map<Identifier, ParticleType<?>> entries = new LinkedHashMap<>();
            for (net.minecraft.entity.particle.ParticleType legacy : net.minecraft.entity.particle.ParticleType.values()) {
                entries.put(ParticleType.id(legacy), new ParticleType<>(legacy) {});
            }
            particleTypes = new MapRegistry<>(entries);
        }
        return particleTypes;
    }

    private static final class MapRegistry<T> implements DefaultedRegistry<T> {
        private final Map<Identifier, T> byId;
        private final Map<T, Identifier> keys = new IdentityHashMap<>();

        MapRegistry(Map<Identifier, T> byId) {
            this.byId = byId;
            byId.forEach((id, value) -> keys.put(value, id));
        }

        @Override
        public Set<Identifier> keySet() {
            return Collections.unmodifiableSet(byId.keySet());
        }

        @Override
        public T getValue(Identifier id) {
            return byId.get(id);
        }

        @Override
        public Identifier getKey(T value) {
            return keys.get(value);
        }

        @Override
        public Set<Map.Entry<ResourceKey, T>> entrySet() {
            return Set.of();
        }

        @Override
        public Iterator<T> iterator() {
            return byId.values().iterator();
        }

        @Override
        public <U> Stream<U> keys(DynamicOps<U> ops) {
            return Stream.empty();
        }
    }
}
