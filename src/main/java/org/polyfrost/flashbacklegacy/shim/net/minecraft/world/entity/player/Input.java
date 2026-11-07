package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player;

public record Input(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift, boolean sprint) {
}
