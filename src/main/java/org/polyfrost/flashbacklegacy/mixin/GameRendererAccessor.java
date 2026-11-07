package org.polyfrost.flashbacklegacy.mixin;

import java.util.Random;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor
    Random getRandom();

    @Accessor
    void setTicks(int ticks);

    @Accessor
    void setFogBrightness(float brightness);

    @Accessor
    void setLastFogBrightness(float brightness);

    @Accessor
    void setLightMapFlicker(float flicker);

    @Accessor
    void setLightMapFlickerTotal(float total);
}
