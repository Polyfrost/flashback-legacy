package org.polyfrost.flashbacklegacy.mixin;

import com.moulberry.flashback.state.EditorState;
import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.entity.particle.ParticleType;
import org.polyfrost.flashbacklegacy.editor.EditorUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
    private void flashback$filter(int id, double x, double y, double z, double vx, double vy, double vz, int[] params, CallbackInfoReturnable<Particle> cir) {
        EditorState state = EditorUi.editorState();
        if (state == null) return;
        if (!state.replayVisuals.renderParticles) {
            cir.setReturnValue(null);
            return;
        }
        ParticleType type = state.filteredParticles.isEmpty() ? null : ParticleType.byId(id);
        if (type != null && state.filteredParticles.contains(org.polyfrost.flashbacklegacy.shim.net.minecraft.core.particles.ParticleType.id(type).toString())) cir.setReturnValue(null);
    }
}
