package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.flag.FeatureElement;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.entity.EntityTypeTest;

import java.util.Locale;

public class EntityType<T> implements EntityTypeTest, FeatureElement {
    public final String key;

    public EntityType(String key) {
        this.key = key;
    }

    public static Identifier id(String key) {
        return Identifier.withDefaultNamespace(key.toLowerCase(Locale.ROOT));
    }

    public String getDescriptionId() {
        return "entity." + key + ".name";
    }
}
