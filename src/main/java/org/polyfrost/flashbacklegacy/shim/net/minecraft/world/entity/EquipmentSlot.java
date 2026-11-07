package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity;

public enum EquipmentSlot {
    MAINHAND(0, "mainhand"), FEET(1, "feet"), LEGS(2, "legs"), CHEST(3, "chest"), HEAD(4, "head"), BODY(-1, "body"), SADDLE(-1, "saddle");

    public final int index;
    private final String name;

    EquipmentSlot(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
