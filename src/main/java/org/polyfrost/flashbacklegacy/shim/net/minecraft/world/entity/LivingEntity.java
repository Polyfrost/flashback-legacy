package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity;

public class LivingEntity extends Entity {
    private final net.minecraft.entity.living.LivingEntity living;

    public LivingEntity(net.minecraft.entity.living.LivingEntity handle) {
        super(handle);
        living = handle;
    }

    public float get$yHeadRot() {
        return living.headYaw;
    }

    public void set$yHeadRotO(float yaw) {
        living.lastHeadYaw = yaw;
    }

    public void set$yRotO(float yaw) {
        living.lastYaw = yaw;
    }

    public void set$xRotO(float pitch) {
        living.lastPitch = pitch;
    }

    public void set$lerpHeadSteps(int steps) {
    }
}
