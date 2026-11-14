package org.polyfrost.flashbacklegacy.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.moulberry.flashback.visuals.ReplayVisuals;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.world.WorldRenderer;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @WrapWithCondition(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FI)V"))
    private boolean flashback$blockEntity(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, float tickDelta, int miningProgress) {
        ReplayVisuals visuals = EditorUi.visuals();
        return visuals == null || visuals.renderBlocks && (visuals.renderBeaconBeams || !(blockEntity instanceof BeaconBlockEntity));
    }

    @Inject(method = "isNear", at = @At("HEAD"), cancellable = true)
    private void flashback$rebuildNow(CallbackInfoReturnable<Boolean> cir) {
        if (EditorUi.exportSettings() != null) cir.setReturnValue(true);
    }
}
