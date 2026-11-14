package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.flashback.io.ReplayCombiner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(value = ReplayCombiner.class, remap = false)
public class ReplayCombinerMixin {
    @WrapOperation(method = "combine", at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;copy(Ljava/nio/file/Path;Ljava/io/OutputStream;)J", ordinal = 0))
    private static long flashback$copyIcon(Path icon, OutputStream out, Operation<Long> original) {
        return Files.exists(icon) ? original.call(icon, out) : 0;
    }
}
