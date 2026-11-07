package org.polyfrost.flashbacklegacy.shim.com.mojang.authlib;

import java.util.UUID;

import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.properties.PropertyMap;

public record GameProfile(UUID id, String name, PropertyMap properties) {
    public static GameProfile of(com.mojang.authlib.GameProfile profile) {
        return new GameProfile(profile.getId(), profile.getName(), new PropertyMap());
    }
}
