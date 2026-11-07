package org.polyfrost.flashbacklegacy.shim.net.minecraft.network;

import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.RegistryAccess;

public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(ByteBuf parent, RegistryAccess registryAccess) {
        super(parent);
        this.registryAccess = registryAccess;
    }

    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess registryAccess) {
        return buf -> new RegistryFriendlyByteBuf(buf, registryAccess);
    }
}
