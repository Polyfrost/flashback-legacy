package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.Utils;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import com.moulberry.flashback.visuals.AccurateEntityPositionHandler;
import com.moulberry.flashback.exporting.ExportSettings;
import com.moulberry.flashback.visuals.ReplayVisuals;
import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.visuals.CameraRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import com.moulberry.flashback.editor.ui.ReplayUI;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private float fogRed;
    @Shadow
    private float fogGreen;
    @Shadow
    private float fogBlue;

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void flashback$replayFov(float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (!changingFov || !FlashbackLegacy.flashbackLoaded || !Flashback.isInReplay()) return;
        if (EditorUi.panoramicExport()) {
            cir.setReturnValue(90f);
            return;
        }
        EditorState state = EditorStateManager.getCurrent();
        cir.setReturnValue(state != null && state.replayVisuals.overrideFov ? state.replayVisuals.overrideFovAmount : minecraft.options.fov);
    }

    @ModifyArg(method = {"setupCamera", "renderItemInHand", "render(IFJ)V", "renderClouds"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false), index = 1)
    private float flashback$panoramicAspect(float aspect) {
        return EditorUi.panoramicExport() ? 1 : aspect;
    }

    @Inject(method = "transformCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;rotatef(FFFF)V", ordinal = 8))
    private void flashback$rollAndShake(float tickDelta, CallbackInfo ci) {
        if (!FlashbackLegacy.flashbackLoaded || !Flashback.isInReplay()) return;
        Quaternionf modifier = CameraRotation.modifyViewQuaternion(new Quaternionf());
        flashback$viewModifier = new Quaternionf(modifier);
        if (modifier.equals(new Quaternionf())) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL11.glMultMatrixf(new Matrix4f().rotation(modifier.conjugate()).get(stack.mallocFloat(16)));
        }
    }

    @Unique
    private Quaternionf flashback$viewModifier;

    @Inject(method = "setupCamera", at = @At("TAIL"))
    private void flashback$storeView(float tickDelta, int anaglyphPass, CallbackInfo ci) {
        Entity camera = minecraft.getCamera();
        if (!FlashbackLegacy.flashbackLoaded || camera == null || !Flashback.isInReplay()) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer projection = stack.mallocFloat(16);
            GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projection);
            ReplayUI.lastProjectionMatrix = new Matrix4f(projection);
        }
        float yaw = camera.lastYaw + (camera.yaw - camera.lastYaw) * tickDelta;
        float pitch = camera.lastPitch + (camera.pitch - camera.lastPitch) * tickDelta;
        Quaternionf view = new Quaternionf().rotationYXZ((float) Math.PI - (float) Math.toRadians(yaw), -(float) Math.toRadians(pitch), 0);
        ReplayUI.lastViewQuaternion = flashback$viewModifier == null ? view : view.mul(flashback$viewModifier);
        flashback$viewModifier = null;
    }

    @ModifyExpressionValue(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientPlayerInteractionManager;hidesGui()Z"))
    private boolean flashback$spectatedHands(boolean hidesGui) {
        ClientPlayerEntity spectated = LegacyReplayServer.spectatedPlayer();
        return spectated != null ? spectated.isSpectator() : hidesGui;
    }

    @WrapWithCondition(method = "render(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/world/WorldRenderer;renderSky(FI)V"))
    private boolean flashback$sky(WorldRenderer worldRenderer, float tickDelta, int anaglyphPass) {
        ReplayVisuals visuals = EditorUi.visuals();
        return visuals == null || visuals.renderSky;
    }

    @WrapOperation(method = "render(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/world/WorldRenderer;render(Lnet/minecraft/client/render/block/BlockLayer;DILnet/minecraft/entity/Entity;)I"))
    private int flashback$terrain(WorldRenderer worldRenderer, BlockLayer layer, double tickDelta, int anaglyphPass, Entity camera, Operation<Integer> original) {
        ReplayVisuals visuals = EditorUi.visuals();
        return visuals == null || visuals.renderBlocks ? original.call(worldRenderer, layer, tickDelta, anaglyphPass, camera) : 0;
    }

    @WrapWithCondition(method = "render(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/world/WorldRenderer;renderMiningProgress(Lnet/minecraft/client/render/vertex/Tesselator;Lnet/minecraft/client/render/vertex/BufferBuilder;Lnet/minecraft/entity/Entity;F)V"))
    private boolean flashback$miningProgress(WorldRenderer worldRenderer, Tesselator tesselator, BufferBuilder builder, Entity camera, float tickDelta) {
        ReplayVisuals visuals = EditorUi.visuals();
        return visuals == null || visuals.renderBlocks;
    }

    @WrapWithCondition(method = "render(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ParticleManager;render(Lnet/minecraft/entity/Entity;F)V"))
    private boolean flashback$particles(ParticleManager particles, Entity camera, float tickDelta) {
        ReplayVisuals visuals = EditorUi.visuals();
        return visuals == null || visuals.renderParticles;
    }

    @WrapWithCondition(method = "render(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ParticleManager;renderLit(Lnet/minecraft/entity/Entity;F)V"))
    private boolean flashback$litParticles(ParticleManager particles, Entity camera, float tickDelta) {
        ReplayVisuals visuals = EditorUi.visuals();
        return visuals == null || visuals.renderParticles;
    }

    @Inject(method = "render(IFJ)V", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
    private void flashback$saveDepth(int anaglyphPass, float tickDelta, long renderTimeLimit, CallbackInfo ci) {
        if (Flashback.EXPORT_JOB != null && Flashback.EXPORT_JOB.isRunning()) Flashback.EXPORT_JOB.tryDepthDownload();
    }

    @Inject(method = "setupClearColor", at = @At("TAIL"))
    private void flashback$clearColour(float tickDelta, CallbackInfo ci) {
        ReplayVisuals visuals = EditorUi.visuals();
        if (visuals == null) return;
        ExportSettings export = EditorUi.exportSettings();
        float[] sky = !visuals.renderSky && export != null && export.transparent() ? new float[3] : visuals.skyColour;
        float[] fog = visuals.overrideFogColour ? visuals.fogColour : visuals.renderSky ? null : sky;
        if (fog != null) {
            fogRed = fog[0];
            fogGreen = fog[1];
            fogBlue = fog[2];
        }
        float[] clear = visuals.renderSky ? fog : sky;
        if (clear != null) GlStateManager.clearColor(clear[0], clear[1], clear[2], 0);
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void flashback$fogDistance(int mode, float tickDelta, CallbackInfo ci) {
        ReplayVisuals visuals = EditorUi.visuals();
        if (visuals == null || !visuals.overrideFog) return;
        GlStateManager.fogMode(GL11.GL_LINEAR);
        GlStateManager.fogStart(visuals.overrideFogStart);
        GlStateManager.fogEnd(visuals.overrideFogEnd);
    }

    @ModifyExpressionValue(method = "updateLightMap", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/living/player/LocalClientPlayerEntity;hasStatusEffect(Lnet/minecraft/entity/living/effect/StatusEffect;)Z"))
    private boolean flashback$nightVision(boolean hasEffect) {
        ReplayVisuals visuals = EditorUi.visuals();
        return visuals != null && visuals.overrideNightVision || hasEffect;
    }

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private void flashback$fullNightVision(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        ReplayVisuals visuals = EditorUi.visuals();
        if (visuals != null && visuals.overrideNightVision) cir.setReturnValue(1f);
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void flashback$accuratePositions(float tickDelta, long renderTimeLimit, CallbackInfo ci) {
        if (!FlashbackLegacy.flashbackLoaded) return;
        var shim = org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.getInstance();
        if (Flashback.RECORDER != null && minecraft.player != null) Flashback.RECORDER.trackPartialPosition(shim.get$player(), tickDelta);
        AccurateEntityPositionHandler.apply(shim.get$level(), runsNormally -> tickDelta);
    }

    @ModifyExpressionValue(method = "render(FJ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;mouseSensitivity:F", opcode = Opcodes.GETFIELD))
    private float flashback$sensitivity(float sensitivity) {
        ReplayVisuals visuals = EditorUi.visuals();
        if (visuals == null || !visuals.overrideFov) return sensitivity;
        float fov = visuals.overrideFovAmount;
        GameOptions options = minecraft.options;
        if (fov > 20 || fov >= options.fov) return sensitivity;
        return sensitivity * Utils.fovToFocalLength(options.fov) / Utils.fovToFocalLength(fov);
    }

    @WrapOperation(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/living/player/LocalClientPlayerEntity;updateLocalPlayerCamera(FF)V"))
    private void flashback$lockFlightRotation(LocalClientPlayerEntity player, float yaw, float pitch, Operation<Void> original) {
        if (EditorUi.exportSettings() != null) return;
        if (FlashbackLegacy.flashbackLoaded && Flashback.isInReplay()) {
            FlashbackConfigV1 config = Flashback.getConfig();
            if (config.editorMovement.flightLockYaw) yaw = 0;
            if (config.editorMovement.flightLockPitch) pitch = 0;
        }
        original.call(player, yaw, pitch);
    }
}
