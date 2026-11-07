package org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

public enum PlayerModelPart {
    CAPE("cape"), JACKET("jacket"), LEFT_SLEEVE("left_sleeve"), RIGHT_SLEEVE("right_sleeve"), LEFT_PANTS_LEG("left_pants_leg"), RIGHT_PANTS_LEG("right_pants_leg"), HAT("hat");

    private final String id;

    PlayerModelPart(String id) {
        this.id = id;
    }

    public Component getName() {
        return Component.translatable("options.modelPart." + id);
    }
}
