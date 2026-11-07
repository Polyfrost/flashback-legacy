package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.moulberry.flashback.Utils;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.ChatFormatting;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Utils.class, remap = false)
public abstract class UtilsMixin {
    @Overwrite
    public static boolean isComponentEmpty(Component component) {
        return ChatFormatting.stripFormatting(component.getString()).isEmpty();
    }
}
