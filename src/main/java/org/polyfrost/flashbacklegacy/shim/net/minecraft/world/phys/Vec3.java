package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.Position;

public class Vec3 implements Position {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public final double x;
    public final double y;
    public final double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(Vector3fc v) {
        this(v.x(), v.y(), v.z());
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }

    public Vec3 add(Vec3 o) {
        return add(o.x, o.y, o.z);
    }

    public Vec3 subtract(double dx, double dy, double dz) {
        return add(-dx, -dy, -dz);
    }

    public Vec3 subtract(Vec3 o) {
        return subtract(o.x, o.y, o.z);
    }

    public Vec3 scale(double s) {
        return new Vec3(x * s, y * s, z * s);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalize() {
        double len = length();
        return len < 1.0E-5 ? ZERO : scale(1 / len);
    }

    public Vec3 lerp(Vec3 to, double t) {
        return new Vec3(x + (to.x - x) * t, y + (to.y - y) * t, z + (to.z - z) * t);
    }

    public double distanceTo(Vec3 o) {
        return subtract(o).length();
    }

    public Vector3f toVector3f() {
        return new Vector3f((float) x, (float) y, (float) z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
