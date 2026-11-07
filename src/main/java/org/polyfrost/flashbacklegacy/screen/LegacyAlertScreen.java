package org.polyfrost.flashbacklegacy.screen;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens.AlertScreen;

public class LegacyAlertScreen extends Screen {
    private final Screen parent;
    private final AlertScreen alert;
    private List<String> lines = List.of();

    public LegacyAlertScreen(Screen parent, AlertScreen alert) {
        this.parent = parent;
        this.alert = alert;
    }

    @Override
    public void init() {
        lines = textRenderer.split(alert.message.text().getFormattedString(), width - 50);
        int y = Math.min(height - 24, height / 2 + lines.size() * textRenderer.fontHeight / 2 + 8);
        buttons.add(new ButtonWidget(0, width / 2 - 100, y, I18n.translate("gui.back")));
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        minecraft.openScreen(parent);
        alert.callback.run();
    }

    @Override
    protected void keyPressed(char chr, int key) {
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        renderBackground();
        int y = height / 2 - lines.size() * textRenderer.fontHeight / 2;
        drawCenteredString(textRenderer, alert.title().text().getFormattedString(), width / 2, Math.max(8, y - 16), 0xFFFFFF);
        for (String line : lines) {
            drawCenteredString(textRenderer, line, width / 2, y, 0xFFFFFF);
            y += textRenderer.fontHeight;
        }
        super.render(mouseX, mouseY, tickDelta);
    }
}
