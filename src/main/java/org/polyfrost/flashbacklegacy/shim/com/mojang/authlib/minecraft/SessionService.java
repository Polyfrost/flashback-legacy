package org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.minecraft;

import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.services.ProfileResult;

import java.util.UUID;

public interface SessionService {
    ProfileResult fetchProfile(UUID id, boolean requireSecure);
}
