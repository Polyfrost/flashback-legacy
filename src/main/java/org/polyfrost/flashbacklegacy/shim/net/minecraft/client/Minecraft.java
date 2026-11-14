package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

import net.minecraft.client.gui.screen.menu.TitleScreen;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.GameProfile;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform.TextInputManager;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform.Window;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.Font;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.Gui;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ClientLevel;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ClientPacketListener;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ServerData;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.player.LocalPlayer;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.server.IntegratedServer;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.sounds.SoundManager;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.server.Services;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.server.packs.resources.ResourceManager;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    public static final Queue<Runnable> SCHEDULED = new ConcurrentLinkedQueue<>();
    private static final KeyboardHandler KEYBOARD = new KeyboardHandler();
    private static final MouseHandler MOUSE = new MouseHandler();
    private final Gui gui = new Gui();
    private final DeltaTracker.Timer deltaTracker = new DeltaTracker.Timer();
    private final GameRenderer gameRenderer = new GameRenderer();

    public static Minecraft getInstance() {
        return INSTANCE;
    }

    private static net.minecraft.client.Minecraft mc() {
        return net.minecraft.client.Minecraft.getInstance();
    }

    public Font get$font() {
        return Font.INSTANCE;
    }

    public DeltaTracker.Timer get$deltaTracker() {
        return deltaTracker;
    }

    public Gui get$gui() {
        return gui;
    }

    public GameRenderer get$gameRenderer() {
        return gameRenderer;
    }

    public Options get$options() {
        return Options.of(mc().options);
    }

    public ClientLevel get$level() {
        return ClientLevel.of(mc().world);
    }

    public LocalPlayer get$player() {
        return (LocalPlayer) Entity.of(mc().player);
    }

    public Window getWindow() {
        return Window.INSTANCE;
    }

    public TextInputManager textInputManager() {
        return TextInputManager.INSTANCE;
    }

    public KeyboardHandler get$keyboardHandler() {
        return KEYBOARD;
    }

    public MouseHandler get$mouseHandler() {
        return MOUSE;
    }

    public MultiPlayerGameMode get$gameMode() {
        var manager = mc().interactionManager;
        return manager == null ? null : new MultiPlayerGameMode(manager);
    }

    public Services services() {
        return Services.CLIENT;
    }

    public ClientPacketListener getConnection() {
        var handler = mc().getNetworkHandler();
        return handler == null ? null : new ClientPacketListener(handler);
    }

    public SoundManager getSoundManager() {
        return SoundManager.INSTANCE;
    }

    public ResourceManager getResourceManager() {
        return ResourceManager.CLASSPATH;
    }

    public GameProfile getGameProfile() {
        return GameProfile.of(mc().getSession().getProfile());
    }

    public void schedule(Runnable task) {
        SCHEDULED.add(task);
    }

    public CompletableFuture<Void> submit(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SCHEDULED.add(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public void disconnectWithProgressScreen() {
        SCHEDULED.add(() -> {
            mc().setWorld(null);
            mc().openScreen(new TitleScreen());
        });
    }

    public ServerData getCurrentServer() {
        var entry = mc().getCurrentServerEntry();
        return entry == null ? null : new ServerData(entry);
    }

    public boolean hasSingleplayerServer() {
        return mc().isIntegratedServerRunning();
    }

    public IntegratedServer getSingleplayerServer() {
        return mc().getServer() instanceof LegacyReplayServer server ? server.bridge : null;
    }

    public boolean isPaused() {
        return mc().isPaused();
    }

    public Entity getCameraEntity() {
        return Entity.of(mc().getCamera());
    }

    public void setCameraEntity(Entity entity) {
        mc().setCamera(entity.handle);
    }
}
