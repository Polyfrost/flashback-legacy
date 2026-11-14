package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

import net.minecraft.client.Minecraft;
import org.polyfrost.flashbacklegacy.mixin.MinecraftAccessor;

public interface DeltaTracker {
    float getGameTimeDeltaPartialTick(boolean runsNormally);

    class Timer implements DeltaTracker {
        @Override
        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            return ((MinecraftAccessor) Minecraft.getInstance()).getTimer().partialTick;
        }
    }
}
