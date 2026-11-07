package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.moulberry.flashback.FilePlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FilePlayerSkin.class, remap = false)
public interface FilePlayerSkinAccessor {
    @Accessor
    String getPathToSkin();
}
