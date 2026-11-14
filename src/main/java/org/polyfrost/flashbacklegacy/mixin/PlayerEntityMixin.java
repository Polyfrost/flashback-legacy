package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.EnhancedFlight;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.MovementDirection;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.render.model.PlayerModelPart;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerAbilities;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.world.World;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.editor.EntityOverrides;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.player.LocalPlayer;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow
    public PlayerAbilities abilities;
    @Shadow
    protected float flyingSpeed;

    protected PlayerEntityMixin(World world) {
        super(world);
    }

    @Inject(method = "isModelPartVisible", at = @At("HEAD"), cancellable = true)
    private void flashback$modelPart(PlayerModelPart part, CallbackInfoReturnable<Boolean> cir) {
        Boolean shown = EntityOverrides.modelPart((PlayerEntity) (Object) this, part);
        if (shown != null) cir.setReturnValue(shown);
    }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void flashback$enhancedFlight(float sideways, float forward, CallbackInfo ci) {
        if (!((Object) this instanceof LocalClientPlayerEntity) || !FlashbackLegacy.flashbackLoaded || !abilities.flying || vehicle != null || !Flashback.isInReplay()) return;
        FlashbackConfigV1 config = Flashback.getConfig();
        FlashbackConfigV1.SubcategoryEditorMovement movement = config.editorMovement;
        boolean airplane = movement.flightDirection == MovementDirection.CAMERA || movement.flightMomentum < 0.98;
        if (!airplane && !movement.flightLockX && !movement.flightLockY && !movement.flightLockZ) return;
        LocalPlayer player = (LocalPlayer) Entity.of(this);
        float vanillaSpeed = flyingSpeed;
        flyingSpeed = airplane ? EnhancedFlight.getFlightSpeed(player, config) : abilities.getFlySpeed() * (isSprinting() ? 2 : 1);
        EnhancedFlight.doFlight(player, config, new Vec3(sideways, 0, forward), flyingSpeed, this::flashback$travel);
        flyingSpeed = vanillaSpeed;
        ci.cancel();
    }

    private void flashback$travel(Vec3 input) {
        velocityY += input.y * flyingSpeed;
        super.moveRelative((float) input.x, (float) input.z);
        if (((LocalPlayer) Entity.of(this)).isNoGravity()) velocityY += 0.08 * 0.98;
    }
}
