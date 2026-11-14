package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.client.render.block.entity.EndPortalRenderer;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.world.WorldRenderer;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin({ItemRenderer.class, EndPortalRenderer.class, WorldRenderer.class})
public abstract class RealClockMixin {
    @ModifyExpressionValue(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getTime()J"))
    private long flashback$replayClock(long time) {
        return EditorUi.renderClock(time);
    }
}
