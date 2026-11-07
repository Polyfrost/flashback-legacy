package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.debug;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.Level;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.chunk.LevelChunk;

public interface DebugScreenEntry {
    void display(DebugScreenDisplayer displayer, Level serverOrClientLevel, LevelChunk clientChunk, LevelChunk serverChunk);
}
