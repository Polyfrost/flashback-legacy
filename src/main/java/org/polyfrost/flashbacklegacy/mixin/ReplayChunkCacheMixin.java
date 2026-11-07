package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.chunk.ServerChunkCache;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkCache.class)
public abstract class ReplayChunkCacheMixin {
    @Shadow
    private ServerWorld world;

    @Redirect(method = "loadChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSource;getChunk(II)Lnet/minecraft/world/chunk/WorldChunk;"))
    private WorldChunk flashback$generate(ChunkSource generator, int x, int z) {
        if (!(world.getServer() instanceof LegacyReplayServer)) return generator.getChunk(x, z);
        WorldChunk chunk = new WorldChunk(world, x, z);
        chunk.setTerrainPopulated(true);
        chunk.setLightPopulated(true);
        ((WorldChunkAccessor) chunk).setHasTicked(true);
        return chunk;
    }
}
