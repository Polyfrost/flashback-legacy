package org.polyfrost.flashbacklegacy.shim.net.minecraft.core.particles;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

public abstract class ParticleType<T> {
    public final net.minecraft.entity.particle.ParticleType legacy;

    protected ParticleType(net.minecraft.entity.particle.ParticleType legacy) {
        this.legacy = legacy;
    }

    public static Identifier id(net.minecraft.entity.particle.ParticleType legacy) {
        String key = legacy.getKey();
        return Identifier.withDefaultNamespace(key.endsWith("_") ? key.substring(0, key.length() - 1) : key);
    }
}
