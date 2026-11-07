package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event.Event;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.commands.CommandBuildContext;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.commands.CommandSourceStack;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.commands.Commands;

@FunctionalInterface
public interface CommandRegistrationCallback {
    Event<CommandRegistrationCallback> EVENT = new Event<>(listeners -> (dispatcher, context, selection) -> listeners.forEach(l -> l.register(dispatcher, context, selection)));

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment);
}
