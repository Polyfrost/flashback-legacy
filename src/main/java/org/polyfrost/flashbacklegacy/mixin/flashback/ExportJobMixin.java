package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.Utils;
import com.moulberry.flashback.combo_options.ExportProjection;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.editor.ui.windows.ExportDoneWindow;
import com.moulberry.flashback.exporting.AsyncFFmpegVideoWriter;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportJobQueue;
import com.moulberry.flashback.exporting.ExportSettings;
import com.moulberry.flashback.exporting.ImageFrame;
import com.moulberry.flashback.exporting.VideoWriter;
import com.moulberry.flashback.keyframe.handler.MinecraftKeyframeHandler;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.sound.FlashbackAudioManager;
import com.moulberry.flashback.state.EditorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.editor.LoopbackAudio;
import org.polyfrost.flashbacklegacy.mixin.EntityAccessor;
import org.polyfrost.flashbacklegacy.mixin.GameRendererAccessor;
import org.polyfrost.flashbacklegacy.mixin.MinecraftAccessor;
import org.polyfrost.flashbacklegacy.mixin.TextureAtlasAccessor;
import org.polyfrost.flashbacklegacy.mixin.TextureAtlasSpriteAccessor;
import org.polyfrost.flashbacklegacy.mixin.WorldRendererAccessor;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform.NativeImage;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

@Mixin(value = ExportJob.class, remap = false)
public abstract class ExportJobMixin {
    @Shadow @Final private ExportSettings settings;
    @Shadow private boolean running;
    @Shadow private boolean shouldChangeFramebufferSize;
    @Shadow @Final private AtomicBoolean finishedServerTick;
    @Shadow public int progressCount;
    @Shadow public int progressOutOf;
    @Shadow private double currentTickDouble;
    @Shadow private int writtenFrames;
    @Shadow private long renderStartTime;
    @Shadow @Final private Random particleRandom;
    @Shadow private long currentClientTickSeed;
    @Shadow private long currentInitialEntityTickSeed;
    @Shadow public Runnable saveDepthAfterLevel;

    @Shadow
    private static List<?> calculateTicks(EditorState editorState, int startTick, int endTick, double fps) {
        throw new AssertionError();
    }

    @Unique private NativeImage flashback$firstFrame;
    @Unique private long flashback$lastProgressMillis;
    @Unique private double flashback$audioSamples;
    @Unique private ImageFrame flashback$depthFrame;

    @Overwrite
    public void run() {
        ReplayServer replayServer = Flashback.getReplayServer();
        if (running || replayServer == null) throw new IllegalStateException("run() called twice");
        running = true;
        Minecraft mc = Minecraft.getInstance();
        mc.unlockMouse();
        mc.getSoundManager().stop();
        boolean oldHideGui = mc.options.hideGui;
        boolean imageSequence = settings.container().isImageSequence() && settings.pngSequenceFormat() != null;
        Path tempFile = imageSequence ? null : Path.of("replay_export_temp/" + UUID.randomUUID() + "." + settings.container().extension());
        boolean keepTempFile = false;
        if (settings.recordAudio()) LoopbackAudio.begin(settings.stereoAudio());
        try {
            if (tempFile != null) Files.createDirectories(tempFile.getParent());
            try (VideoWriter writer = new AsyncFFmpegVideoWriter(settings, tempFile != null ? tempFile.toString() : flashback$sequenceName())) {
                flashback$doExport(mc, replayServer, writer);
            }
            Path output = settings.output();
            boolean errorMoving = false;
            if (tempFile != null) {
                try {
                    Files.move(tempFile, output, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Flashback.LOGGER.error("Error while moving temp file to export output", e);
                    output = tempFile;
                    errorMoving = true;
                    keepTempFile = true;
                }
            }
            long size = settings.container() != VideoContainer.PNG_SEQUENCE && Files.isRegularFile(output) ? Files.size(output) : 0;
            ExportDoneWindow.addFinishedExportEntry(new ExportDoneWindow.FinishedExportEntry(settings, output, errorMoving, flashback$firstFrame, writtenFrames / settings.framerate(), size));
            flashback$firstFrame = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            running = false;
            shouldChangeFramebufferSize = false;
            mc.options.hideGui = oldHideGui;
            EditorUi.updateGameSize();
            replayServer.replayPaused = true;
            mc.getSoundManager().stop();
            LoopbackAudio.end();
            if (tempFile != null) {
                try {
                    if (!keepTempFile) Files.deleteIfExists(tempFile);
                    Files.deleteIfExists(tempFile.getParent());
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Unique
    private String flashback$sequenceName() {
        Path out = settings.output();
        boolean dir = Files.isDirectory(out);
        String formatter = (dir ? "" : out.getFileName() + "-") + settings.pngSequenceFormat();
        String extension = "." + settings.container().extension();
        if (!formatter.endsWith(extension)) formatter += extension;
        return (dir ? out : out.getParent()).resolve(formatter).toString();
    }

    @Unique
    private void flashback$doExport(Minecraft mc, ReplayServer server, VideoWriter writer) {
        LegacyReplayServer.separateViewerTicks = false;
        Random random = new Random(1000L);
        Random mathRandom = settings.resetRng() ? Utils.getInternalMathRandom() : null;
        flashback$setup(mc, server, random, mathRandom);
        flashback$updateRandoms(mc, random, mathRandom);
        mc.options.hideGui = settings.noGui();
        shouldChangeFramebufferSize = true;
        EditorUi.updateGameSize();

        boolean panoramic = EditorUi.panoramicExport();
        List<?> ticks = calculateTicks(settings.editorState(), settings.startTick(), settings.endTick(), settings.framerate());
        renderStartTime = System.currentTimeMillis();
        int clientTickCount = 0;
        for (int index = 0; index < ticks.size(); index++) {
            double[] tick = flashback$tickInfo(ticks.get(index));
            currentTickDouble = tick[0];
            flashback$serverTick(server, settings.startTick() + (int) currentTickDouble, false);
            for (; clientTickCount < (int) tick[1]; clientTickCount++) flashback$seededClientTick(mc, random, mathRandom);
            float partial = (float) (tick[1] - (int) tick[1]);
            boolean frozen = tick[2] != 0;
            if (frozen) FlashbackAudioManager.pauseAll();
            else FlashbackAudioManager.startHandling();
            try {
                settings.editorState().applyKeyframes(new MinecraftKeyframeHandler(org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.getInstance()), (float) (settings.startTick() + currentTickDouble));
            } finally {
                if (!frozen) FlashbackAudioManager.finishHandling();
            }

            int dummyFrames = index == 0 ? Math.max(60, Flashback.getConfig().exporting.exportRenderDummyFrames) : Flashback.getConfig().exporting.exportRenderDummyFrames;
            for (int i = 0; i < dummyFrames; i++) {
                if (mc.screen != null) mc.openScreen(null);
                (panoramic ? flashback$renderPanorama(mc, partial) : flashback$render(mc, partial)).close();
                if (index == 0) flashback$progress(mc, List.of("Warming up... " + (i + 1) + "/" + dummyFrames), true);
            }

            ImageFrame frame = panoramic ? flashback$renderPanorama(mc, partial) : flashback$render(mc, partial);
            frame.audioBuffer = flashback$audio();
            if (!settings.transparent()) frame.makeOpaque();
            if (flashback$firstFrame == null) flashback$firstFrame = frame.toOpaqueRgbaU8NativeImage();
            writtenFrames++;
            writer.encode(frame);

            progressCount = index;
            progressOutOf = ticks.size();
            if (flashback$progressNormal(mc, index, ticks.size())) {
                ExportJobQueue.drainingQueue = false;
                break;
            }
        }
        long finishStart = System.currentTimeMillis();
        writer.finish(info -> flashback$progress(mc, List.of("Finalizing video (" + info + ")... " + (System.currentTimeMillis() - finishStart) / 1000 + "s"), false));
    }

    @Unique
    private FloatBuffer flashback$audio() {
        if (!settings.recordAudio()) return null;
        flashback$audioSamples += 48000.0 / settings.framerate();
        int samples = (int) flashback$audioSamples;
        flashback$audioSamples -= samples;
        return LoopbackAudio.render(samples);
    }

    @Unique
    private void flashback$updateRandoms(Minecraft mc, Random random, Random mathRandom) {
        if (!settings.resetRng()) return;
        random.nextLong();
        long levelSeed = random.nextLong();
        long entitySeed = random.nextLong();
        long mathSeed = random.nextLong();
        long particleSeed = random.nextLong();
        currentClientTickSeed = random.nextLong();
        currentInitialEntityTickSeed = random.nextLong();
        if (mc.world != null) {
            mc.world.random.setSeed(levelSeed);
            ((GameRendererAccessor) mc.gameRenderer).getRandom().setSeed(levelSeed);
            var replay = (LegacyReplayServer) mc.getServer();
            mc.world.getData().setTime(replay.recordedTime());
            mc.world.setTimeOfDay(replay.recordedTimeOfDay());
            ((WorldRendererAccessor) mc.worldRenderer).setTicks((int) mc.world.getTime());
            for (Entity entity : mc.world.entities) ((EntityAccessor) entity).getRandom().setSeed(entitySeed ^ entity.getUuid().getMostSignificantBits());
        }
        if (mathRandom != null) mathRandom.setSeed(mathSeed);
        particleRandom.setSeed(particleSeed);
    }

    @Unique
    private static void flashback$face(LocalClientPlayerEntity player, int face) {
        player.yaw = player.lastYaw = face < 4 ? 90f * face - 90f : 0f;
        player.pitch = player.lastPitch = face < 4 ? 0f : face == 4 ? -90f : 90f;
    }

    @Unique
    private ImageFrame flashback$renderPanorama(Minecraft mc, float partial) {
        LocalClientPlayerEntity player = mc.player;
        float yaw = player.yaw, lastYaw = player.lastYaw, pitch = player.pitch, lastPitch = player.lastPitch;
        ImageFrame[] faces = new ImageFrame[6];
        for (int i = 0; i < 6; i++) {
            flashback$face(player, i);
            faces[i] = flashback$render(mc, partial);
        }
        player.yaw = yaw;
        player.lastYaw = lastYaw;
        player.pitch = pitch;
        player.lastPitch = lastPitch;

        int resX = settings.resolutionX(), resY = settings.resolutionY();
        ImageFrame target = new ImageFrame(resX, resY, faces[0].format, true);
        if (settings.projection() == ExportProjection.CUBE_MAP) {
            for (int i = 0; i < 6; i++) {
                int x = i < 4 ? resX * i / 4 : resX / 4;
                int y = i < 4 ? resY / 3 : i == 4 ? 0 : resY * 2 / 3;
                faces[i].copyRect(target, 0, 0, x, y, Math.min(faces[i].width, resX - x), Math.min(faces[i].height, resY - y));
            }
        } else {
            for (int y = 0; y < resY; y++) {
                for (int x = 0; x < resX; x++) {
                    double yaw2 = (double) x / resX * 2.0 * Math.PI;
                    double pitch2 = (double) y / resY * Math.PI - Math.PI / 2;
                    double sx = -Math.sin(yaw2) * Math.cos(pitch2), sy = Math.sin(pitch2), sz = -Math.cos(yaw2) * Math.cos(pitch2);
                    double a = Math.max(Math.abs(sx), Math.max(Math.abs(sy), Math.abs(sz)));
                    double cx = sx / a, cy = sy / a, cz = sz / a;
                    ImageFrame image;
                    double u, v;
                    boolean flipU = false, flipV = false;
                    if (cy == -1.0) { image = faces[4]; u = cx; v = cz; }
                    else if (cy == 1.0) { image = faces[5]; u = cx; v = cz; flipV = true; }
                    else if (cz == -1.0) { image = faces[3]; u = cx; v = cy; flipU = true; }
                    else if (cz == 1.0) { image = faces[1]; u = cx; v = cy; }
                    else if (cx == 1.0) { image = faces[2]; u = cz; v = cy; flipU = true; }
                    else { image = faces[0]; u = cz; v = cy; }
                    int ix = (int) Math.round((u + 1.0) / 2.0 * (image.width - 1));
                    int iy = (int) Math.round((v + 1.0) / 2.0 * (image.height - 1));
                    image.copyPixel(target, flipU ? image.width - 1 - ix : ix, flipV ? image.height - 1 - iy : iy, x, y);
                }
            }
        }
        for (ImageFrame face : faces) face.close();
        return target;
    }

    @Unique
    private static double[] flashback$tickInfo(Object info) {
        try {
            RecordComponent[] parts = info.getClass().getRecordComponents();
            double[] values = new double[3];
            for (int i = 0; i < 3; i++) {
                var accessor = parts[i].getAccessor();
                accessor.setAccessible(true);
                Object value = accessor.invoke(info);
                values[i] = value instanceof Boolean flag ? flag ? 1 : 0 : (double) value;
            }
            return values;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Unique
    private void flashback$setup(Minecraft mc, ReplayServer server, Random random, Random mathRandom) {
        server.setDesiredTickRate(20, true);
        if (settings.resetRng() || server.getReplayTick() != settings.startTick()) {
            int current = Math.max(0, settings.startTick() - 40);
            if (settings.resetRng()) {
                flashback$serverTick(server, current + 1, true);
                flashback$clientTick(mc);
                flashback$placeCamera(mc);
                Vec3 pos = settings.initialCameraPosition();
                ((LegacyReplayServer) mc.getServer()).resetViewerChunks(pos.x, pos.y, pos.z);
                flashback$serverTick(server, current + 1, true);
                flashback$awaitChunks(mc, server);
                flashback$clientTick(mc);
                flashback$updateRandoms(mc, random, mathRandom);
                var renderer = (GameRendererAccessor) mc.gameRenderer;
                renderer.setTicks(0);
                renderer.setFogBrightness(1);
                renderer.setLastFogBrightness(1);
                renderer.setLightMapFlicker(0);
                renderer.setLightMapFlickerTotal(0);
                flashback$resetTextureAnimations(mc);
            }
            flashback$serverTick(server, current, true);
            flashback$seededClientTick(mc, random, mathRandom);
            mc.particleManager.setWorld(mc.world);
            while (current < settings.startTick()) {
                flashback$serverTick(server, ++current, true);
                flashback$seededClientTick(mc, random, mathRandom);
            }
            flashback$placeCamera(mc);
            settings.editorState().applyKeyframes(new MinecraftKeyframeHandler(org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.getInstance()), settings.startTick());
            flashback$seededClientTick(mc, random, mathRandom);
            flashback$serverTick(server, settings.startTick(), true);
            flashback$seededClientTick(mc, random, mathRandom);
            flashback$awaitChunks(mc, server);
        }
        mc.openScreen(null);
    }

    @Unique
    private static void flashback$resetTextureAnimations(Minecraft mc) {
        var atlas = mc.getBlocksAtlas();
        GlStateManager.bindTexture(atlas.getGlId());
        for (TextureAtlasSprite sprite : ((TextureAtlasAccessor) atlas).getAnimatedSprites()) {
            if (sprite.getClass() != TextureAtlasSprite.class) continue;
            var frame = (TextureAtlasSpriteAccessor) sprite;
            while (frame.getActiveFrame() != 0 || frame.getFrameTicks() != 0) sprite.tick();
        }
    }

    @Unique
    private void flashback$awaitChunks(Minecraft mc, ReplayServer server) {
        var viewers = mc.getServer().getPlayerManager().getAll();
        while (viewers.stream().anyMatch(viewer -> !viewer.pendingChunks.isEmpty())) {
            flashback$serverTick(server, server.getReplayTick(), true);
            flashback$runTasks(mc);
        }
    }

    @Unique
    private void flashback$placeCamera(Minecraft mc) {
        LocalClientPlayerEntity player = mc.player;
        if (player == null) return;
        Vec3 pos = settings.initialCameraPosition();
        player.x = player.lastX = player.prevX = pos.x;
        player.y = player.lastY = player.prevY = pos.y;
        player.z = player.lastZ = player.prevZ = pos.z;
        player.yaw = player.lastYaw = settings.initialCameraYaw();
        player.pitch = player.lastPitch = settings.initialCameraPitch();
        player.velocityX = player.velocityY = player.velocityZ = 0;
    }

    @Unique
    private void flashback$serverTick(ReplayServer server, int target, boolean force) {
        if (!force && server.getReplayTick() == target) return;
        finishedServerTick.set(false);
        LegacyReplayServer.clientSynced = LegacyReplayServer.serverSynced = false;
        Minecraft.getInstance().getNetworkHandler().sendPacket(new KeepAliveC2SPacket(LegacyReplayServer.SYNC_ID));
        server.jumpToTick = target;
        server.replayPaused = true;
        server.sendFinishedServerTick.set(true);
        while (!finishedServerTick.compareAndExchange(true, false)) LockSupport.parkNanos("waiting for server thread", 100_000L);
        while (!LegacyReplayServer.serverSynced) LockSupport.parkNanos("waiting for this tick's packets", 100_000L);
    }

    @Unique
    private void flashback$seededClientTick(Minecraft mc, Random random, Random mathRandom) {
        flashback$updateRandoms(mc, random, mathRandom);
        flashback$clientTick(mc);
    }

    @Unique
    private static void flashback$clientTick(Minecraft mc) {
        flashback$runTasks(mc);
        mc.tick();
    }

    @Unique
    private static void flashback$runTasks(Minecraft mc) {
        var tasks = ((MinecraftAccessor) mc).getTasks();
        synchronized (tasks) {
            while (!tasks.isEmpty()) tasks.poll().run();
        }
    }

    @Unique
    private ImageFrame flashback$render(Minecraft mc, float partial) {
        flashback$runTasks(mc);
        RenderTarget target = mc.getRenderTarget();
        GlStateManager.pushMatrix();
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        target.bindWrite(true);
        GlStateManager.enableTexture();
        if (settings.depthMap()) saveDepthAfterLevel = () -> flashback$depthFrame = flashback$readDepth(target);
        mc.gameRenderer.render(partial, System.nanoTime());
        GlStateManager.popMatrix();
        if (flashback$depthFrame != null) {
            target.unbindWrite();
            ImageFrame frame = flashback$depthFrame;
            flashback$depthFrame = null;
            return frame;
        }

        int w = target.viewWidth, h = target.viewHeight;
        ByteBuffer pixels = MemoryUtil.memAlloc(w * h * 4);
        try {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            target.unbindWrite();
            int scale = settings.ssaa() ? 2 : 1;
            int outW = w / scale, outH = h / scale;
            ImageFrame frame = new ImageFrame(outW, outH, ImageFrame.Format.RGBA_U8, false);
            long src = MemoryUtil.memAddress(pixels);
            if (scale == 1) {
                for (int y = 0; y < h; y++) MemoryUtil.memCopy(src + (long) (h - 1 - y) * w * 4, frame.pixels + (long) y * w * 4, w * 4L);
            } else {
                for (int y = 0; y < outH; y++) {
                    for (int x = 0; x < outW; x++) {
                        int r = 0, g = 0, b = 0, a = 0;
                        for (int dy = 0; dy < 2; dy++) for (int dx = 0; dx < 2; dx++) {
                            long p = src + ((long) (h - 1 - (y * 2 + dy)) * w + x * 2 + dx) * 4;
                            r += MemoryUtil.memGetByte(p) & 255;
                            g += MemoryUtil.memGetByte(p + 1) & 255;
                            b += MemoryUtil.memGetByte(p + 2) & 255;
                            a += MemoryUtil.memGetByte(p + 3) & 255;
                        }
                        MemoryUtil.memPutInt(frame.pixels + ((long) y * outW + x) * 4, (a / 4) << 24 | (b / 4) << 16 | (g / 4) << 8 | r / 4);
                    }
                }
            }
            return frame;
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    @Unique
    private ImageFrame flashback$readDepth(RenderTarget target) {
        int w = target.viewWidth, h = target.viewHeight;
        FloatBuffer depth = MemoryUtil.memAllocFloat(w * h);
        try {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, w, h, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
            float near = ReplayUI.lastProjectionMatrix.m32() / (ReplayUI.lastProjectionMatrix.m22() - 1);
            float far = ReplayUI.lastProjectionMatrix.m32() / (ReplayUI.lastProjectionMatrix.m22() + 1);
            int scale = settings.ssaa() ? 2 : 1;
            int outW = w / scale, outH = h / scale;
            ImageFrame frame = new ImageFrame(outW, outH, ImageFrame.Format.GRAY_F32, false);
            for (int y = 0; y < outH; y++) {
                for (int x = 0; x < outW; x++) {
                    float sum = 0;
                    for (int dy = 0; dy < scale; dy++) for (int dx = 0; dx < scale; dx++) {
                        float d = depth.get((h - 1 - (y * scale + dy)) * w + x * scale + dx);
                        sum += near / (far * (1 - d) + near * d);
                    }
                    MemoryUtil.memPutFloat(frame.pixels + ((long) y * outW + x) * 4, sum / (scale * scale));
                }
            }
            return frame;
        } finally {
            MemoryUtil.memFree(depth);
        }
    }

    @Unique
    private boolean flashback$progressNormal(Minecraft mc, int frame, int total) {
        List<String> lines = new ArrayList<>();
        lines.add("Exported Frames: " + frame + "/" + total);
        long elapsed = System.currentTimeMillis() - renderStartTime;
        lines.add("Time elapsed: " + elapsed / 1000 + "s");
        lines.add(frame >= settings.framerate() ? "Estimated time remaining: " + elapsed * (total - frame) / frame / 1000 + "s" : "Estimated time remaining: ~");
        lines.add("Hold Escape to cancel");
        return flashback$progress(mc, lines, false);
    }

    @Unique
    private boolean flashback$progress(Minecraft mc, List<String> lines, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - flashback$lastProgressMillis < 16) return false;
        flashback$lastProgressMillis = now;
        int width = Display.getWidth(), height = Display.getHeight();
        GlStateManager.viewport(0, 0, width, height);
        GlStateManager.clearColor(0, 0, 0, 1);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, width / 2.0, height / 2.0, 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translatef(0, 0, -2000);
        GlStateManager.enableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableDepthTest();
        int y = height / 4 - lines.size() * 6;
        for (String line : lines) {
            mc.textRenderer.drawWithShadow(line, width / 4f - mc.textRenderer.getWidth(line) / 2f, y, 0xFFFFFF);
            y += 12;
        }
        GlStateManager.disableBlend();
        GlStateManager.enableDepthTest();
        mc.updateDisplay();
        while (Keyboard.next());
        while (Mouse.next());
        return Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
    }
}
