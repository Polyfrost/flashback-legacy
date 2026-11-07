package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity;

public class EntityHitResult extends HitResult {
    private final Entity entity;

    public EntityHitResult(Entity entity, Vec3 location) {
        super(location);
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
