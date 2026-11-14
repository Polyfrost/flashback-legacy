package org.polyfrost.flashbacklegacy.shim.net.minecraft.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.minecraft.SessionService;
import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.services.ProfileResult;

public record Services(SessionService sessionService) {
    public static final Services CLIENT = new Services((id, secure) -> {
        GameProfile profile = Minecraft.getInstance().getSessionService().fillProfileProperties(new GameProfile(id, null), secure);
        return profile.getName() == null ? null : new ProfileResult(org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.GameProfile.of(profile));
    });
}
