package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event.Event;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.commands.CommandBuildContext;

@FunctionalInterface
public interface ClientCommandRegistrationCallback {
    Event<ClientCommandRegistrationCallback> EVENT = new Event<>(listeners -> (dispatcher, context) -> listeners.forEach(l -> l.register(dispatcher, context)));

    void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess);
}
