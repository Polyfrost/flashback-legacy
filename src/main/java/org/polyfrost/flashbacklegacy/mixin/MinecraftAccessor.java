package org.polyfrost.flashbacklegacy.mixin;

import java.util.Queue;
import java.util.concurrent.FutureTask;

import net.minecraft.client.Minecraft;
import net.minecraft.client.TickTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    Queue<FutureTask<?>> getTasks();

    @Accessor
    TickTimer getTimer();

    @Invoker
    void invokeResize(int width, int height);
}
