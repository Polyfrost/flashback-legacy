package org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.minecraft;

import java.util.UUID;
import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.services.ProfileResult;

public interface SessionService {
    ProfileResult fetchProfile(UUID id, boolean requireSecure);
}
