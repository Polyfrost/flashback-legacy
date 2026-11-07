package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.render.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureAtlasSprite.class)
public interface TextureAtlasSpriteAccessor {
    @Accessor
    int getActiveFrame();

    @Accessor
    int getFrameTicks();
}
