package org.polyfrost.flashbacklegacy.mixin;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "forceEntity", at = @At("HEAD"))
    private void flashback$stableUuid(int id, Entity entity, CallbackInfo ci) {
        LegacyReplayServer.stabilizeUuid(entity, id);
    }

    @Redirect(method = "doRandomDisplayTicks", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;randomDisplayTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/BlockState;Ljava/util/Random;)V"))
    private void flashback$seededDisplayTick(Block block, World world, BlockPos pos, BlockState state, Random random) {
        block.randomDisplayTick(world, pos, state, EditorUi.displayTickRandom(random, pos));
    }
}
