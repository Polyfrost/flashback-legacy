package org.polyfrost.flashbacklegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.polyfrost.flashbacklegacy.record.LegacyPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @WrapOperation(method = "finishMiningBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;doEvent(ILnet/minecraft/util/math/BlockPos;I)V"))
    private void flashback$recordBreak(World world, int type, BlockPos pos, int data, Operation<Void> original) {
        LegacyPacket.record(new WorldEventS2CPacket(type, pos, data, false));
        original.call(world, type, pos, data);
    }
}
