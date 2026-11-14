package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.text.Formatting;
import net.minecraft.text.LiteralText;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;

public final class ClientCommands {
    private static CommandDispatcher<FabricClientCommandSource> dispatcher;

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static boolean execute(String command) {
        try {
            if (dispatcher == null) {
                CommandDispatcher<FabricClientCommandSource> built = new CommandDispatcher<>();
                ClientCommandRegistrationCallback.EVENT.invoker().register(built, null);
                dispatcher = built;
            }
            ParseResults<FabricClientCommandSource> parse = dispatcher.parse(command, null);
            if (parse.getContext().getNodes().isEmpty()) return false;
            dispatcher.execute(parse);
        } catch (CommandSyntaxException e) {
            error(e.getMessage());
        } catch (Throwable t) {
            FlashbackLegacy.LOGGER.error("Client command /{} failed", command, t);
            error(t.toString());
        }
        return dispatcher != null;
    }

    private static void error(String message) {
        LiteralText text = new LiteralText(message);
        text.getStyle().setColor(Formatting.RED);
        Minecraft.getInstance().gui.getChat().addMessage(text);
    }
}
