package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

public abstract class Screen extends AbstractContainerEventHandler {
    public static final Screen VANILLA = new Screen(Component.empty()) {
    };

    protected final Component title;

    protected Screen(Component title) {
        this.title = title;
    }

    protected Screen() {
        this(Component.empty());
    }

    public boolean isPauseScreen() {
        return true;
    }
}
