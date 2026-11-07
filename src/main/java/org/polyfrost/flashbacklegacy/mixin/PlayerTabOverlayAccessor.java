package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.gui.overlay.PlayerTabOverlay;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
    @Accessor
    Text getHeader();

    @Accessor
    Text getFooter();
}
