package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.FreezeSlowdownFormula;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportJobQueue;
import com.moulberry.flashback.exporting.PerfectFrames;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.keyframe.handler.MinecraftKeyframeHandler;
import com.moulberry.flashback.keyframe.handler.TickrateKeyframeCapture;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.sound.FlashbackAudioManager;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TickTimer;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldSettings;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    private TickTimer timer;
    @Shadow
    public ClientWorld world;
    @Shadow
    public LocalClientPlayerEntity player;

    @Unique
    private final TickTimer viewerTimer = new TickTimer(20);
    @Unique
    private boolean wasInReplay;
    @Shadow
    private boolean paused;
    @Unique
    private long flashback$clientTicks;
    @Unique
    private long flashback$freezeClientStart = -1;
    @Unique
    private int flashback$freezeServerStart = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void flashback$startTick(CallbackInfo ci) {
        flashback$clientTicks++;
        var shim = org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.getInstance();
        for (Runnable task; (task = org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.SCHEDULED.poll()) != null; ) {
            try {
                task.run();
            } catch (LinkageError e) {
                FlashbackLegacy.LOGGER.error("Scheduled Flashback task hit a missing shim", e);
            }
        }
        ClientTickEvents.START_CLIENT_TICK.invoker().onStartTick(shim);
    }

    @WrapOperation(method = "startGame", at = @At(value = "NEW", target = "net/minecraft/server/integrated/IntegratedServer"))
    private IntegratedServer flashback$replayServer(Minecraft minecraft, String saveName, String worldName, WorldSettings settings, Operation<IntegratedServer> original) {
        return LegacyReplayServer.pending != null ? new LegacyReplayServer(minecraft, saveName, worldName, settings) : original.call(minecraft, saveName, worldName, settings);
    }

    @Inject(method = "setWorld(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V", at = @At("HEAD"))
    private void flashback$finishRecording(ClientWorld world, String message, CallbackInfo ci) {
        if (!FlashbackLegacy.flashbackLoaded || world != null || Flashback.RECORDER == null || !Flashback.getConfig().recordingControls.automaticallyFinish) return;
        try {
            Flashback.finishRecordingReplay();
        } catch (Exception e) {
            FlashbackLegacy.LOGGER.error("Could not finish recording on disconnect", e);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void flashback$endTick(CallbackInfo ci) {
        ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.getInstance());
        if (!FlashbackLegacy.flashbackLoaded) return;
        if (Flashback.RECORDER != null) Flashback.RECORDER.endTickWithContext(false);
        EditorStateManager.saveIfNeeded();
        boolean inReplay = Flashback.getReplayServer() != null;
        if (inReplay != wasInReplay && !(wasInReplay = inReplay)) EditorStateManager.reset();
        Minecraft mc = (Minecraft) (Object) this;
        if (inReplay && !Flashback.getConfig().advanced.disableThirdPersonCancel && ReplayUI.isActive()
                && player != null && mc.getCamera() == player && mc.options.perspective != 0) {
            mc.options.perspective = 0;
            ReplayUI.setInfoOverlay("Forced perspective to First-Person");
        }
    }

    @Inject(method = "runGame", at = @At("HEAD"))
    private void flashback$runExport(CallbackInfo ci) {
        if (!FlashbackLegacy.flashbackLoaded) return;
        if (ExportJobQueue.drainingQueue) {
            if (ExportJobQueue.queuedJobs.isEmpty()) ExportJobQueue.drainingQueue = false;
            else if (Flashback.EXPORT_JOB == null) Flashback.EXPORT_JOB = new ExportJob(ExportJobQueue.queuedJobs.removeFirst());
        }
        if (Flashback.EXPORT_JOB != null && !ReplayUI.isActive()) {
            try {
                PerfectFrames.enable();
                Flashback.EXPORT_JOB.run();
            } finally {
                PerfectFrames.disable();
                Flashback.EXPORT_JOB = null;
            }
        }
    }

    @Inject(method = "runGame", at = @At("HEAD"))
    private void flashback$tickRate(CallbackInfo ci) {
        ReplayServer replay = FlashbackLegacy.flashbackLoaded && world != null ? Flashback.getReplayServer() : null;
        LegacyReplayServer.worldHeld = false;
        timer.tpsScale = replay == null ? 1 : flashback$tickScale(replay);
        if (LegacyReplayServer.worldHeld) paused = true;
        LegacyReplayServer.separateViewerTicks = flashback$overridingViewerTimer();
    }

    @Unique
    private float flashback$tickScale(ReplayServer replay) {
        float scale = replay.tickRate() / 20;
        EditorState state = EditorStateManager.getCurrent();
        if (state != null && !replay.replayPaused) {
            double partialReplayTick = replay.getPartialReplayTick();
            TickrateKeyframeCapture capture = new TickrateKeyframeCapture();
            state.applyKeyframes(capture, (float) partialReplayTick);
            if (capture.frozen && capture.frozenDelay > 0) {
                if (flashback$freezeServerStart < 0) {
                    flashback$freezeClientStart = flashback$clientTicks + 1;
                    flashback$freezeServerStart = (int) partialReplayTick;
                }
                double delta = partialReplayTick - flashback$freezeServerStart;
                if (delta >= 0 && delta <= capture.frozenDelay) {
                    double freezeTicks = capture.frozenDelay <= 5 ? 0.999 : 1.999;
                    double base = FreezeSlowdownFormula.getFreezePowerBase(capture.frozenDelay, capture.frozenDelay <= 5 ? 1 : 0.5);
                    double target = freezeTicks * FreezeSlowdownFormula.calculateFreezeClientTick(delta, capture.frozenDelay, base);
                    double current = flashback$clientTicks + timer.partialTick - flashback$freezeClientStart;
                    return scale * (float) Math.clamp(target - current, 0.01, 1);
                }
            } else {
                flashback$freezeServerStart = -1;
            }
        } else {
            flashback$freezeServerStart = -1;
        }
        if (!replay.isFrozen()) return scale;
        LegacyReplayServer.worldHeld = true;
        return 1;
    }

    @Inject(method = "runGame", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=preRenderErrors"), cancellable = true)
    private void flashback$afterTicks(CallbackInfo ci) {
        ReplayServer replay = FlashbackLegacy.flashbackLoaded ? Flashback.getReplayServer() : null;
        if (replay == null) {
            if (FlashbackLegacy.flashbackLoaded) FlashbackAudioManager.stopAll();
            return;
        }
        if (flashback$overridingViewerTimer()) {
            viewerTimer.advance();
            int ticks = Math.min(10, viewerTimer.ticksThisFrame);
            if (!paused || LegacyReplayServer.worldHeld && ((Minecraft) (Object) this).screen == null) {
                for (int i = 0; i < ticks; i++) LegacyReplayServer.tickViewer(world, player);
            }
        }
        boolean force = replay.forceApplyKeyframes.compareAndSet(true, false);
        boolean replayPaused = replay.replayPaused;
        if (replayPaused) FlashbackAudioManager.pauseAll();
        if (!replayPaused || force) {
            if (!replayPaused) FlashbackAudioManager.startHandling();
            try {
                replay.getEditorState().applyKeyframes(new MinecraftKeyframeHandler(org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.getInstance()), (float) replay.getPartialReplayTick());
            } catch (RuntimeException | LinkageError e) {
                LegacyReplayServer.logOnce("client keyframes", e);
            } finally {
                if (!replayPaused) FlashbackAudioManager.finishHandling();
            }
        }
        if (!replay.doClientRendering()) ci.cancel();
    }

    @Inject(method = "runGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V"))
    private void flashback$drawEditor(CallbackInfo ci) {
        EditorUi.draw();
    }

    @ModifyVariable(method = "resize", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int flashback$frameWidth(int width) {
        return EditorUi.gameWidth(width);
    }

    @ModifyVariable(method = "resize", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int flashback$frameHeight(int height) {
        return EditorUi.gameHeight(height);
    }

    @WrapOperation(method = "runGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/pipeline/RenderTarget;draw(II)V"))
    private void flashback$drawGame(RenderTarget target, int width, int height, Operation<Void> original) {
        EditorUi.drawGame(width, height, () -> original.call(target, width, height));
    }

    @Inject(method = "lockMouse", at = @At("HEAD"), cancellable = true)
    private void flashback$editorOwnsMouse(CallbackInfo ci) {
        if (EditorUi.blocksMouseLock()) ci.cancel();
    }

    @WrapOperation(method = "runGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(FJ)V"))
    private void flashback$render(GameRenderer renderer, float partialTick, long nanoTime, Operation<Void> original) {
        if (player == null || !flashback$overridingViewerTimer()) {
            original.call(renderer, partialTick, nanoTime);
            return;
        }
        LocalClientPlayerEntity p = player;
        double[] saved = {p.x, p.y, p.z, p.lastX, p.lastY, p.lastZ, p.prevX, p.prevY, p.prevZ};
        float t = viewerTimer.partialTick;
        p.x = p.lastX = p.prevX = saved[3] + (saved[0] - saved[3]) * t;
        p.y = p.lastY = p.prevY = saved[4] + (saved[1] - saved[4]) * t;
        p.z = p.lastZ = p.prevZ = saved[5] + (saved[2] - saved[5]) * t;
        try {
            original.call(renderer, partialTick, nanoTime);
        } finally {
            p.x = saved[0];
            p.y = saved[1];
            p.z = saved[2];
            p.lastX = saved[3];
            p.lastY = saved[4];
            p.lastZ = saved[5];
            p.prevX = saved[6];
            p.prevY = saved[7];
            p.prevZ = saved[8];
        }
    }

    @Unique
    private boolean flashback$overridingViewerTimer() {
        return world != null && player != null && player.vehicle == null && !player.removed && (timer.tpsScale != 1 || LegacyReplayServer.worldHeld);
    }
}
