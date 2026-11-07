package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.visuals.ReplayVisuals;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.world.WorldRenderer;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FI)V"))
    private void flashback$blockEntity(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, float tickDelta, int miningProgress) {
        ReplayVisuals visuals = EditorUi.visuals();
        if (visuals != null && (!visuals.renderBlocks || !visuals.renderBeaconBeams && blockEntity instanceof BeaconBlockEntity)) return;
        dispatcher.render(blockEntity, tickDelta, miningProgress);
    }

    @Inject(method = "isNear", at = @At("HEAD"), cancellable = true)
    private void flashback$rebuildNow(CallbackInfoReturnable<Boolean> cir) {
        if (EditorUi.exportSettings() != null) cir.setReturnValue(true);
    }
}
