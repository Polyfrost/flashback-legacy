package org.polyfrost.flashbacklegacy.playback;

import java.util.List;
import java.util.UUID;

import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.AbstractCommand;
import net.minecraft.server.command.exception.CommandException;
import net.minecraft.server.command.exception.IncorrectUsageException;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.text.TranslatableText;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft;

final class HideCommand extends AbstractCommand {
    private final boolean hide;

    HideCommand(boolean hide) {
        this.hide = hide;
    }

    @Override
    public String getName() {
        return hide ? "hide" : "show";
    }

    @Override
    public String getUsage(CommandSource source) {
        return "/" + getName() + " <targets>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void run(CommandSource source, String[] args) throws CommandException {
        if (args.length != 1) throw new IncorrectUsageException(getUsage(source));
        List<UUID> ids = parseEntities(source, args[0]).stream().map(Entity::getUuid).toList();
        Minecraft.SCHEDULED.add(() -> {
            EditorState state = EditorStateManager.getCurrent();
            if (state == null) return;
            if (hide) state.hideDuringExport.addAll(ids);
            else ids.forEach(state.hideDuringExport::remove);
        });
        source.sendMessage(new TranslatableText(hide ? "flashback.hide_command.n_entities_hidden" : "flashback.show_command.n_entities_shown", ids.size()));
    }
}
