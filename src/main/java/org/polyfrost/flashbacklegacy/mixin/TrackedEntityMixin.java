package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.TrackedEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TrackedEntity.class)
public abstract class TrackedEntityMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/FilledMapItem;getUpdatePacket(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/living/player/PlayerEntity;)Lnet/minecraft/network/packet/Packet;"))
    private Packet<?> flashback$noBlankMaps(FilledMapItem item, ItemStack stack, World world, PlayerEntity player) {
        return world instanceof ServerWorld server && server.getServer() instanceof LegacyReplayServer ? null : item.getUpdatePacket(stack, world, player);
    }
}
