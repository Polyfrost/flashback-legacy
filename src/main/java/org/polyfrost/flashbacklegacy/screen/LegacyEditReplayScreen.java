package org.polyfrost.flashbacklegacy.screen;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.moulberry.flashback.FlashbackGson;
import com.moulberry.flashback.record.FlashbackMeta;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens.AlertScreen;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

public class LegacyEditReplayScreen extends Screen {
    private static final int BACK = 0, SAVE = 1, COMBINE = 2;

    private final Screen parent;
    private final Path replay;
    private final FlashbackMeta metadata;
    private TextFieldWidget name;
    private ButtonWidget save;

    public LegacyEditReplayScreen(Screen parent, Path replay, FlashbackMeta metadata) {
        this.parent = parent;
        this.replay = replay;
        this.metadata = metadata;
    }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        String text = name == null ? metadata.name : name.getText();
        name = new TextFieldWidget(3, textRenderer, width / 2 - 100, height / 2 - 30, 200, 20);
        name.setMaxLength(128);
        name.setText(text);
        name.setFocused(true);
        buttons.add(new ButtonWidget(COMBINE, width / 2 - 100, height / 2 + 4, I18n.translate("flashback.combine_with_other_replay")));
        buttons.add(save = new ButtonWidget(SAVE, width / 2 - 100, height / 2 + 28, 98, 20, I18n.translate("flashback.save_changes")));
        buttons.add(new ButtonWidget(BACK, width / 2 + 2, height / 2 + 28, 98, 20, I18n.translate("gui.back")));
        tick();
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void tick() {
        name.tick();
        save.active = !name.getText().equals(metadata.name);
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        switch (button.id) {
            case SAVE -> applyChanges();
            case COMBINE -> minecraft.openScreen(new LegacyCombineReplayScreen(this, replay));
            case BACK -> minecraft.openScreen(parent);
        }
    }

    private void applyChanges() {
        metadata.name = name.getText();
        try (FileSystem fileSystem = FileSystems.newFileSystem(replay)) {
            Files.writeString(fileSystem.getPath("/metadata.json"), FlashbackGson.PRETTY.toJson(metadata.toJson()));
        } catch (IOException e) {
            FlashbackLegacy.LOGGER.error("Unable to edit replay", e);
            minecraft.openScreen(new LegacyAlertScreen(parent, new AlertScreen(() -> {}, Component.translatable("flashback.unable_to_edit_replay"), Component.literal(e.toString()))));
        }
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) minecraft.openScreen(parent);
        else name.keyPressed(chr, key);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        name.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        renderBackground();
        drawCenteredString(textRenderer, I18n.translate("flashback.edit_replay"), width / 2, height / 2 - 70, 0xFFFFFF);
        drawString(textRenderer, I18n.translate("flashback.replay_name"), width / 2 - 100, height / 2 - 42, 0xA0A0A0);
        name.render();
        super.render(mouseX, mouseY, tickDelta);
    }
}
