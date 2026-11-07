package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui;

import com.moulberry.flashback.screen.RecoverRecordingsScreen;
import com.moulberry.flashback.screen.SaveReplayScreen;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.screen.LegacyAlertScreen;
import org.polyfrost.flashbacklegacy.screen.LegacyRecoverRecordingsScreen;
import org.polyfrost.flashbacklegacy.screen.LegacySaveReplayScreen;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.toasts.ToastManager;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens.AlertScreen;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens.Overlay;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens.Screen;

public class Gui {
    private final Hud hud = new Hud();

    public Hud get$hud() {
        return hud;
    }

    public Screen screen() {
        return net.minecraft.client.Minecraft.getInstance().screen == null ? null : Screen.VANILLA;
    }

    public void setScreen(Screen screen) {
        if (screen == null || screen == Screen.VANILLA) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        switch (screen) {
            case SaveReplayScreen save -> mc.openScreen(new LegacySaveReplayScreen(mc.screen, save));
            case RecoverRecordingsScreen recover -> mc.openScreen(new LegacyRecoverRecordingsScreen(mc.screen, recover));
            case AlertScreen alert -> mc.openScreen(new LegacyAlertScreen(mc.screen, alert));
            default -> FlashbackLegacy.LOGGER.warn("Flashback tried to open {}, which isn't ported yet", screen.getClass().getName());
        }
    }

    public ToastManager toastManager() {
        return null;
    }

    public Overlay overlay() {
        return null;
    }
}
