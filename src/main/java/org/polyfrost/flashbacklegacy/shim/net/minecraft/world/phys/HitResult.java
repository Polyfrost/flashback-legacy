package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys;

public abstract class HitResult {
    protected final Vec3 location;

    protected HitResult() {
        this(null);
    }

    protected HitResult(Vec3 location) {
        this.location = location;
    }

    public Vec3 getLocation() {
        return location;
    }
}
