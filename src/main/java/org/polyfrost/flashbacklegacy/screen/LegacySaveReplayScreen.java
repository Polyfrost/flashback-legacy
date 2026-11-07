package org.polyfrost.flashbacklegacy.screen;

import com.moulberry.flashback.screen.SaveReplayScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;
import org.polyfrost.flashbacklegacy.mixin.flashback.SaveReplayScreenInvoker;

public class LegacySaveReplayScreen extends Screen {
    private static final int SAVE = 0, DELETE = 1;

    private final Screen parent;
    private final SaveReplayScreenInvoker replay;
    private TextFieldWidget name;

    public LegacySaveReplayScreen(Screen parent, SaveReplayScreen replay) {
        this.parent = parent;
        this.replay = (SaveReplayScreenInvoker) replay;
    }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        String text = name == null ? replay.flashback$getReplayName() : name.getText();
        name = new TextFieldWidget(2, textRenderer, width / 2 - 100, height / 2 - 30, 200, 20);
        name.setMaxLength(256);
        name.setText(text);
        name.setFocused(true);
        buttons.add(new ButtonWidget(SAVE, width / 2 - 102, height / 2 + 4, 100, 20, I18n.translate("flashback.save_replay")));
        buttons.add(new ButtonWidget(DELETE, width / 2 + 2, height / 2 + 4, 100, 20, I18n.translate("flashback.delete_replay")));
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void tick() {
        name.tick();
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id == SAVE) {
            replay.flashback$setReplayName(name.getText());
            minecraft.openScreen(parent);
            replay.flashback$saveReplay();
        } else if (button.id == DELETE) {
            minecraft.openScreen(new ConfirmScreen(this, I18n.translate("flashback.confirm_delete_replay"), I18n.translate("flashback.confirm_delete_replay_message"), 0));
        }
    }

    @Override
    public void confirmResult(boolean confirmed, int id) {
        if (confirmed) replay.flashback$deleteReplay();
        minecraft.openScreen(confirmed ? parent : this);
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key != Keyboard.KEY_ESCAPE) name.keyPressed(chr, key);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        name.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        renderBackground();
        drawCenteredString(textRenderer, I18n.translate("flashback.save_replay"), width / 2, height / 2 - 70, 0xFFFFFF);
        drawString(textRenderer, I18n.translate("flashback.replay_name"), width / 2 - 100, height / 2 - 42, 0xA0A0A0);
        name.render();
        super.render(mouseX, mouseY, tickDelta);
    }
}
