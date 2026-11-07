package org.polyfrost.flashbacklegacy.editor;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.ExportProjection;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportSettings;
import java.util.Random;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import com.moulberry.flashback.editor.ui.CustomImGuiWindowerSdl;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.visuals.ReplayVisuals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFilter;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys.EntityHitResult;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.sdl.SDL_Event;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.mixin.MinecraftAccessor;
import java.util.function.Predicate;
import pl.tomgirl.pylon.window.DisplaySdl;

import static org.lwjgl.sdl.SDLEvents.*;

public final class EditorUi {
    private static boolean started;
    private static boolean failed;
    public static int[] frameViewport;

    private EditorUi() {
    }

    public static void draw() {
        if (failed || !FlashbackLegacy.flashbackLoaded || !(started || Flashback.isInReplay())) return;
        started = true;
        try {
            ReplayUI.drawOverlay();
            syncMouseLock();
        } catch (Throwable t) {
            failed = true;
            FlashbackLegacy.LOGGER.error("Flashback editor UI failed; disabled until restart", t);
        }
    }

    private static void syncMouseLock() {
        if (!ReplayUI.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean moving = ReplayUI.isMovingCamera();
        if (moving && !mc.focused) mc.lockMouse();
        else if (!moving && mc.focused) mc.unlockMouse();
    }

    public static boolean blocksMouseLock() {
        return started && !failed && ReplayUI.isActive() && !ReplayUI.isMovingCamera();
    }

    public static int eventType(SDL_Event event) {
        int type = event.type();
        if (!started || failed) return type;
        try {
            boolean passKey = type == SDL_EVENT_KEY_UP || type == SDL_EVENT_KEY_DOWN && Flashback.isExporting();
            if (((CustomImGuiWindowerSdl) ReplayUI.imguiWindower).processEvent(event) && !passKey) return -1;
            if (ReplayUI.isActive()) {
                if (type == SDL_EVENT_MOUSE_BUTTON_DOWN || type == SDL_EVENT_MOUSE_BUTTON_UP) {
                    event.button().x((float) ReplayUI.getNewMouseX(event.button().x())).y((float) ReplayUI.getNewMouseY(event.button().y()));
                } else if (type == SDL_EVENT_MOUSE_WHEEL) {
                    event.wheel().mouse_x((float) ReplayUI.getNewMouseX(event.wheel().mouse_x())).mouse_y((float) ReplayUI.getNewMouseY(event.wheel().mouse_y()));
                }
            }
            return type;
        } catch (Throwable t) {
            failed = true;
            FlashbackLegacy.LOGGER.error("Flashback editor input failed; disabled until restart", t);
            return type;
        }
    }

    private static boolean modifiesViewport() {
        return started && !failed && ReplayUI.shouldModifyViewport();
    }

    private static float pixelScale(int pixels, int points) {
        return Math.clamp((float) pixels / Math.max(1, points), 0.125f, 8f);
    }

    public static int gameWidth(int displayWidth) {
        ExportJob job = exportJob();
        if (job != null) return job.getWidth();
        return modifiesViewport() ? ReplayUI.getNewGameWidth(pixelScale(Display.getWidth(), DisplaySdl.instance().getWindowWidth())) : displayWidth;
    }

    public static int gameHeight(int displayHeight) {
        ExportJob job = exportJob();
        if (job != null) return job.getHeight();
        return modifiesViewport() ? ReplayUI.getNewGameHeight(pixelScale(Display.getHeight(), DisplaySdl.instance().getWindowHeight())) : displayHeight;
    }

    public static ExportSettings exportSettings() {
        ExportJob job = FlashbackLegacy.flashbackLoaded ? Flashback.EXPORT_JOB : null;
        return job != null && job.isRunning() ? job.getSettings() : null;
    }

    public static long renderClock() {
        ExportSettings settings = exportSettings();
        return settings == null ? Minecraft.getTime() : (long) ((settings.startTick() + Flashback.EXPORT_JOB.getCurrentTickDouble()) * 50);
    }

    public static boolean panoramicExport() {
        ExportSettings settings = exportSettings();
        return settings != null && (settings.projection() == ExportProjection.CUBE_MAP || settings.projection() == ExportProjection.EQUIRECTANGULAR);
    }

    public static Random entityRandom(Entity entity) {
        ExportSettings settings = exportSettings();
        if (settings == null || !settings.resetRng() || !Minecraft.getInstance().isOnSameThread()) return new Random();
        Random particles = Flashback.EXPORT_JOB.getParticleRandom();
        if (entity instanceof Particle && particles != null) return new Random(particles.nextLong());
        return new Random(Flashback.EXPORT_JOB.getInitialEntitySeedForCurrentClientTick());
    }

    public static boolean resetRngExport() {
        ExportSettings settings = exportSettings();
        return settings != null && settings.resetRng();
    }

    public static Random displayTickRandom(Random random, BlockPos pos) {
        return resetRngExport() ? new Random(MathHelper.hashCode(pos) ^ Flashback.EXPORT_JOB.getSeedForCurrentClientTick()) : random;
    }

    private static ExportJob exportJob() {
        ExportJob job = FlashbackLegacy.flashbackLoaded ? Flashback.EXPORT_JOB : null;
        return job != null && job.shouldChangeFramebufferSize() ? job : null;
    }

    public static void updateGameSize() {
        Minecraft mc = Minecraft.getInstance();
        int width = Display.getWidth(), height = Display.getHeight();
        if (gameWidth(width) != mc.width || gameHeight(height) != mc.height) ((MinecraftAccessor) mc).invokeResize(width, height);
    }

    public static int mouseFlipHeight(int displayHeight) {
        return modifiesViewport() ? Minecraft.getInstance().height : displayHeight;
    }

    public static void drawGame(RenderTarget target, int width, int height) {
        if (!modifiesViewport()) {
            target.draw(width, height);
            return;
        }
        int displayWidth = Display.getWidth(), displayHeight = Display.getHeight();
        DisplaySdl display = DisplaySdl.instance();
        float sx = pixelScale(displayWidth, display.getWindowWidth()), sy = pixelScale(displayHeight, display.getWindowHeight());
        GlStateManager.viewport(0, 0, displayWidth, displayHeight);
        GlStateManager.clearColor(0, 0, 0, 1);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        frameViewport = new int[]{Math.round(ReplayUI.frameX * sx), displayHeight - Math.round(ReplayUI.frameY * sy) - height, width, height};
        try {
            target.draw(width, height);
        } finally {
            frameViewport = null;
        }
    }

    public static EditorState editorState() {
        return FlashbackLegacy.flashbackLoaded ? EditorStateManager.getCurrent() : null;
    }

    public static ReplayVisuals visuals() {
        EditorState state = editorState();
        return state == null ? null : state.replayVisuals;
    }

    public static boolean isHidden(Entity entity) {
        EditorState state = editorState();
        return state != null && state.isEntityHidden(org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity.of(entity));
    }

    public static boolean hidesNametag(Entity entity) {
        EditorState state = editorState();
        return state != null && (!state.replayVisuals.renderNametags || state.hideNametags.contains(entity.getUuid()) || isHidden(entity));
    }

    public static boolean hidesHud(Predicate<ReplayVisuals> shown) {
        ReplayVisuals visuals = visuals();
        return visuals != null && !shown.test(visuals) && (ReplayUI.isActive() || Flashback.isExporting());
    }

    public static EntityHitResult lookTarget() {
        Vec3 look = ReplayUI.getMouseLookVector();
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCamera();
        ReplayVisuals visuals = visuals();
        if (look == null || camera == null || mc.world == null || visuals == null) return null;
        if (!visuals.renderEntities && !visuals.renderPlayers) return null;
        Vec3d from = camera.getEyePosition(1);
        double distance = 64;
        Vec3d dir = new Vec3d(look.x * distance, look.y * distance, look.z * distance);
        if (visuals.renderBlocks) {
            HitResult block = mc.world.rayTrace(from, from.add(dir));
            if (block != null && block.type != HitResult.Type.MISS) distance = block.facePos.distanceTo(from);
        }
        Vec3d to = from.add(new Vec3d(look.x * distance, look.y * distance, look.z * distance));
        Entity best = null;
        Vec3d bestPos = null;
        double bestDistance = distance;
        for (Entity entity : mc.world.getEntities(camera, camera.getShape().expanded(dir.x, dir.y, dir.z).grown(1, 1, 1), EntityFilter.NOT_SPECTATOR)) {
            if (entity instanceof PlayerEntity ? !visuals.renderPlayers : !visuals.renderEntities) continue;
            float r = entity.getPickRadius();
            Box box = entity.getShape().grown(r, r, r);
            if (box.contains(from)) return new EntityHitResult(org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity.of(entity), new Vec3(from.x, from.y, from.z));
            HitResult hit = box.clip(from, to);
            if (hit != null && from.distanceTo(hit.facePos) < bestDistance) {
                best = entity;
                bestPos = hit.facePos;
                bestDistance = from.distanceTo(hit.facePos);
            }
        }
        return best == null ? null : new EntityHitResult(org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity.of(best), new Vec3(bestPos.x, bestPos.y, bestPos.z));
    }
}
