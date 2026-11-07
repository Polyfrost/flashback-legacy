package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

public interface DeltaTracker {
    float getGameTimeDeltaPartialTick(boolean runsNormally);

    class Timer implements DeltaTracker {
        @Override
        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            return ((org.polyfrost.flashbacklegacy.mixin.MinecraftAccessor) net.minecraft.client.Minecraft.getInstance()).getTimer().partialTick;
        }
    }
}
