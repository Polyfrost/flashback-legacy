package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.network.packet.Packet;
import net.minecraft.server.TrackedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TrackedEntity.class)
public interface TrackedEntityInvoker {
    @Invoker
    Packet<?> invokeCreateAddEntityPacket();
}
