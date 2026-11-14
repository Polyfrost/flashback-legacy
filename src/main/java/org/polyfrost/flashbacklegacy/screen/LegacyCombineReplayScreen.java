package org.polyfrost.flashbacklegacy.screen;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.io.ReplayCombiner;
import com.moulberry.flashback.utils.AsyncFileDialogs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.screens.AlertScreen;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LegacyCombineReplayScreen extends Screen {
    private static final int FIRST = 0, SECOND = 1, OUTPUT = 2, COMBINE = 3, CANCEL = 4;

    private final Screen parent;
    private final Path[] paths = new Path[3];
    private final ButtonWidget[] pickers = new ButtonWidget[3];
    private TextFieldWidget name;
    private ButtonWidget combine;

    public LegacyCombineReplayScreen(Screen parent, Path first) {
        this.parent = parent;
        paths[FIRST] = first;
    }

    private int top() {
        return height / 2 - 100;
    }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        String text = name == null ? "Combined Replay" : name.getText();
        name = new TextFieldWidget(5, textRenderer, width / 2 - 100, top() + 27, 200, 20);
        name.setMaxLength(128);
        name.setText(text);
        name.setFocused(true);
        for (int i = FIRST; i <= OUTPUT; i++) {
            buttons.add(pickers[i] = new ButtonWidget(i, width / 2 - 100, top() + 62 + i * 35, ""));
        }
        buttons.add(combine = new ButtonWidget(COMBINE, width / 2 - 100, top() + 164, 98, 20, I18n.translate("flashback.combine_replay.do_combine")));
        buttons.add(new ButtonWidget(CANCEL, width / 2 + 2, top() + 164, 98, 20, I18n.translate("gui.cancel")));
        tick();
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void tick() {
        name.tick();
        for (int i = FIRST; i <= OUTPUT; i++) {
            pickers[i].message = paths[i] == null ? "" : paths[i].getFileName().toString();
        }
        combine.active = Arrays.stream(paths).allMatch(Objects::nonNull);
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        switch (button.id) {
            case FIRST, SECOND, OUTPUT -> pick(button.id);
            case COMBINE -> combine();
            case CANCEL -> minecraft.openScreen(parent);
        }
    }

    private void pick(int index) {
        String folder = Flashback.getReplayFolder().toString();
        CompletableFuture<String> dialog = index == OUTPUT
            ? AsyncFileDialogs.saveFileDialog(folder, "combined.zip", "Replay Archive", new String[]{"zip"})
            : AsyncFileDialogs.openFileDialog(folder, "Replay Archive", new String[]{"zip"});
        dialog.thenAccept(path -> {
            if (path != null) paths[index] = Path.of(path);
        });
    }

    private void combine() {
        Path temp = paths[OUTPUT].resolveSibling(paths[OUTPUT].getFileName() + ".tmp");
        try {
            ReplayCombiner.combine(null, name.getText(), paths[FIRST], paths[SECOND], temp);
            Files.move(temp, paths[OUTPUT], StandardCopyOption.REPLACE_EXISTING);
            minecraft.openScreen(new TitleScreen());
        } catch (Exception e) {
            FlashbackLegacy.LOGGER.error("Error combining replays", e);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException deleteError) {
                e.addSuppressed(deleteError);
            }
            minecraft.openScreen(new LegacyAlertScreen(this, new AlertScreen(() -> {}, Component.translatable("flashback.combine_replay.error"), Component.literal(e.toString()))));
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
        int left = width / 2 - 100;
        drawCenteredString(textRenderer, I18n.translate("flashback.combine_replay"), width / 2, top(), 0xFFFFFF);
        drawString(textRenderer, I18n.translate("flashback.combine_replay.new_replay_name"), left, top() + 16, 0xA0A0A0);
        drawString(textRenderer, I18n.translate("flashback.combine_replay.source_n", "1"), left, top() + 51, 0xA0A0A0);
        drawString(textRenderer, I18n.translate("flashback.combine_replay.source_n", "2"), left, top() + 86, 0xA0A0A0);
        drawString(textRenderer, I18n.translate("flashback.combine_replay.output"), left, top() + 121, 0xA0A0A0);
        name.render();
        super.render(mouseX, mouseY, tickDelta);
    }
}
