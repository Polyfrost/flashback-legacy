package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.rendering.v1.hud;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.DeltaTracker;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.GuiGraphicsExtractor;

@FunctionalInterface
public interface HudElement {
    void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
