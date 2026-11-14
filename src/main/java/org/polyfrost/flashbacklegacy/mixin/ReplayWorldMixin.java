package org.polyfrost.flashbacklegacy.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.moulberry.flashback.combo_options.WeatherOverride;
import com.moulberry.flashback.visuals.ReplayVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockMiningProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.record.LegacyPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class ReplayWorldMixin {
    @WrapWithCondition(method = "tickEntity(Lnet/minecraft/entity/Entity;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;tick()V"))
    private boolean flashback$tickEntity(Entity entity) {
        return !LegacyReplayServer.isReplayEntity(entity) && !LegacyReplayServer.skipClientTick(entity);
    }

    @WrapWithCondition(method = "tickEntity(Lnet/minecraft/entity/Entity;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;rideTick()V"))
    private boolean flashback$rideTick(Entity entity) {
        return !LegacyReplayServer.isReplayEntity(entity) && !LegacyReplayServer.skipClientTick(entity);
    }

    @WrapWithCondition(method = "tickEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Tickable;tick()V"))
    private boolean flashback$tickBlockEntity(Tickable blockEntity) {
        return !((Object) this instanceof ServerWorld world && world.getServer() instanceof LegacyReplayServer);
    }

    @Inject(method = "doEvent(Lnet/minecraft/entity/living/player/PlayerEntity;ILnet/minecraft/util/math/BlockPos;I)V", at = @At("HEAD"))
    private void flashback$recordEvent(PlayerEntity source, int type, BlockPos pos, int data, CallbackInfo ci) {
        if ((Object) this instanceof ClientWorld && source != null && source == Minecraft.getInstance().player) {
            LegacyPacket.record(new WorldEventS2CPacket(type, pos, data, false));
        }
    }

    @Inject(method = "updateBlockMiningProgress", at = @At("HEAD"))
    private void flashback$recordMining(int id, BlockPos pos, int progress, CallbackInfo ci) {
        if ((Object) this instanceof ClientWorld && Minecraft.getInstance().player != null && id == Minecraft.getInstance().player.getNetworkId()) {
            LegacyPacket.record(new BlockMiningProgressS2CPacket(id, pos, progress));
        }
    }

    private ReplayVisuals flashback$clientVisuals() {
        return (Object) this instanceof ClientWorld ? EditorUi.visuals() : null;
    }

    @Inject(method = "getRain", at = @At("HEAD"), cancellable = true)
    private void flashback$rainOverride(float tickDelta, CallbackInfoReturnable<Float> cir) {
        ReplayVisuals visuals = flashback$clientVisuals();
        if (visuals != null && visuals.overrideWeatherMode != WeatherOverride.NONE) cir.setReturnValue(visuals.overrideWeatherMode == WeatherOverride.CLEAR ? 0f : 1f);
    }

    @Inject(method = "getThunder", at = @At("HEAD"), cancellable = true)
    private void flashback$thunderOverride(float tickDelta, CallbackInfoReturnable<Float> cir) {
        ReplayVisuals visuals = flashback$clientVisuals();
        if (visuals != null && visuals.overrideWeatherMode != WeatherOverride.NONE) cir.setReturnValue(visuals.overrideWeatherMode == WeatherOverride.THUNDERING ? 1f : 0f);
    }

    @ModifyExpressionValue(method = {"getTimeOfDay()J", "getTimeOfDay(F)F", "getMoonPhase", "getMoonSize"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldData;getTimeOfDay()J"))
    private long flashback$timeOverride(long time) {
        ReplayVisuals visuals = flashback$clientVisuals();
        return visuals != null && visuals.overrideTimeOfDay >= 0 ? visuals.overrideTimeOfDay : time;
    }
}
