package org.polyfrost.flashbacklegacy.mixin;

import java.util.List;

import net.minecraft.client.world.chunk.ClientChunkCache;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientChunkCache.class)
public interface ClientChunkCacheAccessor {
    @Accessor
    List<WorldChunk> getChunks();
}
