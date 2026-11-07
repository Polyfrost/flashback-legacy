package org.polyfrost.flashbacklegacy.screen;

import java.util.List;

import com.moulberry.flashback.screen.RecoverRecordingsScreen;
import com.moulberry.flashback.screen.RecoverRecordingsScreen.RecoveryOption;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Formatting;
import org.polyfrost.flashbacklegacy.mixin.flashback.RecoverRecordingsScreenAccessor;

public class LegacyRecoverRecordingsScreen extends Screen {
    private final Screen parent;
    private final RecoverRecordingsScreenAccessor recover;
    private List<String> lines = List.of();

    public LegacyRecoverRecordingsScreen(Screen parent, RecoverRecordingsScreen recover) {
        this.parent = parent;
        this.recover = (RecoverRecordingsScreenAccessor) recover;
    }

    @Override
    public void init() {
        lines = textRenderer.split(recover.flashback$getMessageText().text().getFormattedString(), width - 50);
        int y = Math.min(height - 24, height / 2 + lines.size() * textRenderer.fontHeight / 2 + 8);
        add(RecoveryOption.RECOVER, -154, y, Formatting.GREEN, "flashback.recover");
        add(RecoveryOption.SKIP, -50, y, Formatting.YELLOW, "flashback.skip");
        add(RecoveryOption.DELETE, 54, y, Formatting.RED, "flashback.delete");
    }

    private void add(RecoveryOption option, int x, int y, Formatting color, String key) {
        buttons.add(new ButtonWidget(option.ordinal(), width / 2 + x, y, 100, 20, color + I18n.translate(key)));
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        minecraft.openScreen(parent);
        recover.flashback$getHandler().accept(RecoveryOption.values()[button.id]);
    }

    @Override
    protected void keyPressed(char chr, int key) {
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        renderBackground();
        int y = height / 2 - lines.size() * textRenderer.fontHeight / 2;
        drawCenteredString(textRenderer, recover.flashback$getTitle().text().getFormattedString(), width / 2, Math.max(8, y - 16), 0xFFFFFF);
        for (String line : lines) {
            drawCenteredString(textRenderer, line, width / 2, y, 0xFFFFFF);
            y += textRenderer.fontHeight;
        }
        super.render(mouseX, mouseY, tickDelta);
    }
}
