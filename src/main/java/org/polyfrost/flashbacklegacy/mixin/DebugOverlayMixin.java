package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.gui.overlay.DebugOverlay;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.debug.DebugScreenEntries;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.debug.DebugScreenEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugOverlay.class)
public abstract class DebugOverlayMixin {
    @Inject(method = "getGameInfo", at = @At("RETURN"))
    private void flashback$entries(CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = cir.getReturnValue();
        for (DebugScreenEntry entry : DebugScreenEntries.ENTRIES.values()) entry.display((group, line) -> lines.add(line), null, null, null);
    }
}
