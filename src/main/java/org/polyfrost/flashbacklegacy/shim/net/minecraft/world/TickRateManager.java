package org.polyfrost.flashbacklegacy.shim.net.minecraft.world;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.playback.ReplayServer;

public class TickRateManager {
    public float tickrate() {
        ReplayServer replay = Flashback.getReplayServer();
        return replay == null ? 20 : replay.tickRate();
    }
}
