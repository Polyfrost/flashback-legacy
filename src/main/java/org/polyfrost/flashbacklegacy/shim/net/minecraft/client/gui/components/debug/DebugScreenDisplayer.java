package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.debug;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

public interface DebugScreenDisplayer {
    void addToGroup(Identifier group, String line);
}
