package org.polyfrost.flashbacklegacy.mixin.flashback;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.moulberry.flashback.io.ReplayCombiner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ReplayCombiner.class, remap = false)
public class ReplayCombinerMixin {
    @Redirect(method = "combine", at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;copy(Ljava/nio/file/Path;Ljava/io/OutputStream;)J", ordinal = 0))
    private static long flashback$copyIcon(Path icon, OutputStream out) throws IOException {
        return Files.exists(icon) ? Files.copy(icon, out) : 0;
    }
}
