package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.visuals.ReplayVisuals;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.item.ItemStack;
import org.polyfrost.flashbacklegacy.editor.EntityOverrides;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Unique
    private ItemStack[] flashback$hiddenEquipment;

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)Z", at = @At("HEAD"))
    private void flashback$hideEquipment(Entity entity, double x, double y, double z, float yaw, float tickDelta, boolean hitbox, CallbackInfoReturnable<Boolean> cir) {
        flashback$hiddenEquipment = EntityOverrides.hideEquipment(entity);
    }

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)Z", at = @At("RETURN"))
    private void flashback$restoreEquipment(Entity entity, double x, double y, double z, float yaw, float tickDelta, boolean hitbox, CallbackInfoReturnable<Boolean> cir) {
        if (flashback$hiddenEquipment == null) return;
        EntityOverrides.restoreEquipment(entity, flashback$hiddenEquipment);
        flashback$hiddenEquipment = null;
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void flashback$hideEntity(Entity entity, Culler culler, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EditorState state = EditorUi.editorState();
        if (state == null) return;
        ReplayVisuals visuals = state.replayVisuals;
        if (!(entity instanceof PlayerEntity ? visuals.renderPlayers : visuals.renderEntities) || EditorUi.isHidden(entity)) {
            cir.setReturnValue(false);
            return;
        }
        String key = state.filteredEntities.isEmpty() ? null : Entities.getKey(entity);
        if (key != null && state.filteredEntities.contains(EntityType.id(key).toString())) cir.setReturnValue(false);
    }
}
