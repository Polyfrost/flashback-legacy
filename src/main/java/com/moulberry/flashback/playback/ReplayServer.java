package com.moulberry.flashback.playback;

import com.google.gson.JsonObject;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.network.PacketByteBuf;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.server.IntegratedServer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReplayServer extends IntegratedServer {
    public volatile int jumpToTick = -1;
    public volatile boolean replayPaused;
    public final AtomicBoolean forceApplyKeyframes = new AtomicBoolean();
    public final AtomicBoolean sendFinishedServerTick = new AtomicBoolean();
    public volatile boolean isProcessingSnapshot;
    public volatile boolean fastForwarding;
    public volatile boolean failedToLoadRegistryDataWarning;
    public volatile boolean failedToSpawnPlayerWarning;
    public volatile boolean hasServerResourcePack;
    private volatile float desiredTickRate = 20;
    private volatile float desiredTickRateManual = 20;
    private volatile boolean desiredFrozen;
    private volatile int desiredFrozenDelay = -1;
    private volatile boolean frozen;
    private int frozenDelay = -1;
    private volatile long lastTimeRtc;
    private volatile long timeRtc;
    private long pendingTimeRtc;

    private final LegacyReplayServer server;
    private final FlashbackMeta metadata;

    public ReplayServer(LegacyReplayServer server, JsonObject metadata) {
        this.server = server;
        FlashbackMeta meta = FlashbackMeta.fromJson(metadata);
        if (meta == null) {
            meta = new FlashbackMeta();
            meta.replayIdentifier = UUID.randomUUID();
        }
        this.metadata = meta;
    }

    public void goToReplayTick(int tick) {
        jumpToTick = tick;
    }

    public float getDesiredTickRate(boolean manual) {
        return manual ? desiredTickRateManual : desiredTickRate;
    }

    public void setDesiredTickRate(float tickRate, boolean manual) {
        if (manual) desiredTickRateManual = tickRate;
        else desiredTickRate = tickRate;
    }

    public float tickRate() {
        return Math.max(1, desiredTickRate * desiredTickRateManual / 20);
    }

    public void setFrozen(boolean frozen, int delay) {
        desiredFrozen = frozen;
        desiredFrozenDelay = delay;
    }

    public void updateFrozen() {
        if (!desiredFrozen) {
            frozenDelay = -1;
            frozen = false;
            return;
        }
        if (frozenDelay < 0) frozenDelay = desiredFrozenDelay <= 0 ? 1 : desiredFrozenDelay <= 5 ? 2 : 3;
        if (frozenDelay > 0) frozenDelay--;
        if (frozenDelay == 0) frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getReplayTick() {
        return server.targetTick;
    }

    public double getPartialReplayTick() {
        if (replayPaused) return server.targetTick;
        double partial = (System.nanoTime() - server.lastTickNanos) * tickRate() / 1e9;
        return server.targetTick - 1 + Math.clamp(partial, 0, 1);
    }

    public int getTotalReplayTicks() {
        return server.totalTicks;
    }

    public boolean doClientRendering() {
        return !isProcessingSnapshot && !fastForwarding;
    }

    public FlashbackMeta getMetadata() {
        return metadata;
    }

    public EditorState getEditorState() {
        return Flashback.isExporting() ? Flashback.EXPORT_JOB.getSettings().editorState() : EditorStateManager.get(metadata.replayIdentifier);
    }

    public int getLocalPlayerId() {
        return server.localPlayerId();
    }

    public void handleRtc(PacketByteBuf buf) {
        byte delta = buf.readByte();
        if (delta == 0) {
            pendingTimeRtc = buf.readLong();
        } else {
            if (pendingTimeRtc == 0) pendingTimeRtc = timeRtc;
            pendingTimeRtc += 50 + delta;
        }
    }

    public void updateRtc() {
        if (!hasRtcData()) return;
        if (pendingTimeRtc == 0 && timeRtc != 0) pendingTimeRtc = timeRtc + 50;
        lastTimeRtc = lastTimeRtc != 0 && lastTimeRtc <= pendingTimeRtc ? timeRtc : pendingTimeRtc;
        timeRtc = pendingTimeRtc;
        pendingTimeRtc = 0;
    }

    public boolean hasRtcData() {
        return lastTimeRtc != 0 || timeRtc != 0 || pendingTimeRtc != 0;
    }

    public long getInterpolatedRtc() {
        if (replayPaused || frozen || timeRtc < lastTimeRtc) return timeRtc;
        double partial = Math.clamp((System.nanoTime() - server.lastTickNanos) * tickRate() / 1e9, 0, 1);
        return lastTimeRtc + (long) ((timeRtc - lastTimeRtc) * partial);
    }

    public Collection<ReplayPlayer> getReplayViewers() {
        return List.of();
    }
}
