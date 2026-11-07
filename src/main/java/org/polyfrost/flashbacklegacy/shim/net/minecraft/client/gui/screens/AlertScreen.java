package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

public class AlertScreen extends Screen {
    public final Runnable callback;
    public final Component message;

    public AlertScreen(Runnable callback, Component title, Component message) {
        super(title);
        this.callback = callback;
        this.message = message;
    }

    public Component title() {
        return title;
    }
}
