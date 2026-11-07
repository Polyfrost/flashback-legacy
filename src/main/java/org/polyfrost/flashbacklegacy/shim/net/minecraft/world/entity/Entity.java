package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.commands.arguments.EntityAnchorArgument;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.util.Mth;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.entity.EntityAccess;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.MapMaker;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ClientLevel;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.player.LocalPlayer;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.Player;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.Level;

public class Entity implements EntityAccess {
    private static final Map<net.minecraft.entity.Entity, Entity> WRAPPERS = new MapMaker().weakKeys().makeMap();

    public final net.minecraft.entity.Entity handle;

    protected Entity(net.minecraft.entity.Entity handle) {
        this.handle = handle;
    }

    public static Entity of(net.minecraft.entity.Entity handle) {
        if (handle == null) return null;
        return WRAPPERS.computeIfAbsent(handle, h -> switch (h) {
            case LocalClientPlayerEntity p -> new LocalPlayer(p);
            case net.minecraft.client.entity.living.player.ClientPlayerEntity p -> new org.polyfrost.flashbacklegacy.shim.net.minecraft.client.player.AbstractClientPlayer(p);
            case PlayerEntity p -> new Player(p);
            case net.minecraft.entity.living.LivingEntity l -> new LivingEntity(l);
            default -> new Entity(h);
        });
    }

    public int getId() {
        return handle.getNetworkId();
    }

    public UUID getUUID() {
        return handle.getUuid();
    }

    public boolean isRemoved() {
        return handle.removed;
    }

    public double getX() {
        return handle.x;
    }

    public double getY() {
        return handle.y;
    }

    public double getZ() {
        return handle.z;
    }

    public double get$xo() {
        return handle.prevX;
    }

    public double get$yo() {
        return handle.prevY;
    }

    public double get$zo() {
        return handle.prevZ;
    }

    public boolean isPassenger() {
        return handle.vehicle != null;
    }

    public void setXRot(float pitch) {
        handle.pitch = pitch;
    }

    public void setYRot(float yaw) {
        handle.yaw = yaw;
    }

    public void setYHeadRot(float yaw) {
        handle.setHeadYaw(yaw);
    }

    public void snapTo(double x, double y, double z, float yaw, float pitch) {
        handle.setPositionAndAngles(x, y, z, yaw, pitch);
    }

    public float getXRot() {
        return handle.pitch;
    }

    public float getYRot() {
        return handle.yaw;
    }

    public float getViewXRot(float partialTick) {
        return partialTick == 1 ? handle.pitch : Mth.lerp(partialTick, handle.lastPitch, handle.pitch);
    }

    public float getViewYRot(float partialTick) {
        return partialTick == 1 ? handle.yaw : Mth.lerp(partialTick, handle.lastYaw, handle.yaw);
    }

    public float getEyeHeight() {
        return handle.getEyeHeight();
    }

    public Vec3 position() {
        return new Vec3(handle.x, handle.y, handle.z);
    }

    public Vec3 getEyePosition() {
        return new Vec3(handle.x, handle.y + handle.getEyeHeight(), handle.z);
    }

    public Vec3 getDeltaMovement() {
        return new Vec3(handle.velocityX, handle.velocityY, handle.velocityZ);
    }

    public void setDeltaMovement(Vec3 v) {
        handle.velocityX = v.x;
        handle.velocityY = v.y;
        handle.velocityZ = v.z;
    }

    public void snapTo(Vec3 pos) {
        handle.setPositionAndAngles(pos.x, pos.y, pos.z, handle.yaw, handle.pitch);
    }

    public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 target) {
        Vec3 from = anchor == EntityAnchorArgument.Anchor.EYES ? getEyePosition() : position();
        double dx = target.x - from.x, dy = target.y - from.y, dz = target.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        handle.pitch = Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(dy, horizontal)));
        handle.yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90);
        handle.lastPitch = handle.pitch;
        handle.lastYaw = handle.yaw;
    }

    public double distanceToSqr(Entity other) {
        return handle.squaredDistanceTo(other.handle);
    }

    public Level level() {
        return ClientLevel.of((net.minecraft.client.world.ClientWorld) handle.world);
    }

    private boolean noGravity;

    public boolean isNoGravity() {
        return noGravity;
    }

    public void setNoGravity(boolean noGravity) {
        this.noGravity = noGravity;
    }

    public void move(MoverType type, Vec3 movement) {
        handle.move(movement.x, movement.y, movement.z);
    }

    public void setDeltaMovement(double x, double y, double z) {
        handle.velocityX = x;
        handle.velocityY = y;
        handle.velocityZ = z;
    }

    public void setOnGround(boolean onGround) {
        handle.onGround = onGround;
    }

    public void setPos(double x, double y, double z) {
        handle.setPosition(x, y, z);
    }

    public void resetFallDistance() {
        handle.fallDistance = 0;
    }

    public boolean isSprinting() {
        return handle.isSprinting();
    }

    public boolean isFallFlying() {
        return false;
    }

    public void stopFallFlying() {
    }
}
