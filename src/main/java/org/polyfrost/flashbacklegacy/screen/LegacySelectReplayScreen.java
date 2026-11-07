package org.polyfrost.flashbacklegacy.screen;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.FlashbackGson;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.screen.select_replay.NaturalOrderComparator;
import com.moulberry.flashback.screen.select_replay.ReplaySorting;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.Identifier;
import org.lwjgl.input.Keyboard;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.Blaze3D;

public class LegacySelectReplayScreen extends Screen {
    private static final int BACK = 0, OPEN = 1, FOLDER = 2, DELETE = 3, EDIT = 4, SORT = 5, DIRECTION = 6, SEARCH = 7, LOAD = 8, OPTIONS = 9;

    private record Replay(Path path, FlashbackMeta metadata, Identifier icon, boolean folder) {
    }

    private final Screen parent;
    private final Path folder;
    private List<Replay> all = List.of();
    private List<Replay> replays = List.of();
    private int selected = -1;
    private ReplayList list;
    private ButtonWidget open, edit, delete, sort, direction;
    private TextFieldWidget search;

    public LegacySelectReplayScreen(Screen parent) {
        this(parent, Flashback.getReplayFolder());
    }

    private LegacySelectReplayScreen(Screen parent, Path folder) {
        this.parent = parent;
        this.folder = folder;
    }

    @Override
    public void init() {
        closeIcons();
        Keyboard.enableRepeatEvents(true);
        try (Stream<Path> files = Files.list(folder)) {
            all = files.filter(path -> Files.isDirectory(path) || path.getFileName().toString().endsWith(".zip")).map(this::read).toList();
        } catch (IOException e) {
            FlashbackLegacy.LOGGER.error("Could not list replays", e);
        }
        list = new ReplayList();
        String query = search == null ? "" : search.getText();
        search = new TextFieldWidget(SEARCH, textRenderer, width / 2 - 152, 22, 150, 20);
        search.setText(query);
        buttons.add(sort = new ButtonWidget(SORT, width / 2 + 2, 22, 128, 20, ""));
        buttons.add(direction = new ButtonWidget(DIRECTION, width / 2 + 132, 22, 20, 20, ""));
        buttons.add(open = new ButtonWidget(OPEN, width / 2 - 154, height - 52, 100, 20, I18n.translate("flashback.select_replay.open")));
        buttons.add(edit = new ButtonWidget(EDIT, width / 2 - 50, height - 52, 100, 20, I18n.translate("flashback.edit_replay")));
        buttons.add(delete = new ButtonWidget(DELETE, width / 2 + 54, height - 52, 100, 20, I18n.translate("flashback.delete_replay")));
        buttons.add(new ButtonWidget(FOLDER, width / 2 - 154, height - 28, 88, 20, I18n.translate("flashback.open_replays")));
        buttons.add(new ButtonWidget(LOAD, width / 2 - 64, height - 28, 128, 20, I18n.translate("flashback.select_replay.load_replay_from_file")));
        buttons.add(new ButtonWidget(OPTIONS, width - 72, 2, 70, 20, I18n.translate("menu.options")));
        buttons.add(new ButtonWidget(BACK, width / 2 + 66, height - 28, 88, 20, I18n.translate("gui.back")));
        refresh();
    }

    private void refresh() {
        var config = Flashback.getConfig().internal;
        boolean text = config.replaySorting == ReplaySorting.REPLAY_NAME || config.replaySorting == ReplaySorting.WORLD_NAME;
        Comparator<Replay> comparator = switch (config.replaySorting) {
            case CREATED_DATE -> Comparator.comparingLong(replay -> modified(replay.path()));
            case REPLAY_NAME -> Comparator.comparing(LegacySelectReplayScreen::displayName, NaturalOrderComparator.INSTANCE);
            case WORLD_NAME -> Comparator.comparing(replay -> replay.metadata() == null ? null : replay.metadata().worldName, Comparator.nullsLast(NaturalOrderComparator.INSTANCE));
            case DURATION -> Comparator.comparingInt(replay -> replay.metadata() == null ? 0 : replay.metadata().totalTicks);
            case FILESIZE -> Comparator.comparingLong(replay -> replay.path().toFile().length());
        };
        if (config.sortDescending != text) comparator = comparator.reversed();
        String query = search.getText().toLowerCase(Locale.ROOT);
        replays = all.stream().filter(replay -> (displayName(replay) + " " + replay.path().getFileName()).toLowerCase(Locale.ROOT).contains(query)).sorted(comparator).toList();
        sort.message = I18n.translate("flashback.sort") + ": " + config.replaySorting.component().getString();
        direction.message = config.sortDescending ? "v" : "^";
        selected = -1;
        open.active = edit.active = delete.active = false;
    }

    private static String displayName(Replay replay) {
        boolean named = replay.metadata() != null && !replay.metadata().name.isEmpty();
        return named ? replay.metadata().name : replay.path().getFileName().toString().replaceFirst("\\.zip$", "");
    }

    private Replay read(Path path) {
        if (Files.isDirectory(path)) return new Replay(path, null, null, true);
        try (FileSystem fileSystem = FileSystems.newFileSystem(path)) {
            FlashbackMeta metadata = FlashbackMeta.fromJson(FlashbackGson.COMPRESSED.fromJson(Files.readString(fileSystem.getPath("/metadata.json")), JsonObject.class));
            return new Replay(path, metadata, readIcon(fileSystem), false);
        } catch (Exception e) {
            FlashbackLegacy.LOGGER.warn("Could not read metadata of {}", path, e);
            return new Replay(path, null, null, false);
        }
    }

    private Identifier readIcon(FileSystem fileSystem) throws IOException {
        Path icon = fileSystem.getPath("/icon.png");
        if (!Files.exists(icon) || Files.size(icon) == 0) return null;
        try (InputStream in = Files.newInputStream(icon)) {
            BufferedImage image = ImageIO.read(in);
            return image == null ? null : minecraft.getTextureManager().register("flashback_replay", new DynamicTexture(image));
        }
    }

    private void closeIcons() {
        for (Replay replay : all) {
            if (replay.icon() != null) minecraft.getTextureManager().close(replay.icon());
        }
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
        closeIcons();
    }

    @Override
    public void tick() {
        search.tick();
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            minecraft.openScreen(parent);
            return;
        }
        String before = search.getText();
        search.keyPressed(chr, key);
        if (!before.equals(search.getText())) refresh();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        search.mouseClicked(mouseX, mouseY, button);
    }

    private static long modified(Path path) {
        return path.toFile().lastModified();
    }

    private void openSelected() {
        if (selected < 0) return;
        Replay replay = replays.get(selected);
        if (replay.folder()) minecraft.openScreen(new LegacySelectReplayScreen(this, replay.path()));
        else Flashback.openReplayWorld(replay.path().toAbsolutePath());
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        switch (button.id) {
            case OPEN -> openSelected();
            case EDIT -> minecraft.openScreen(new LegacyEditReplayScreen(this, replays.get(selected).path(), replays.get(selected).metadata()));
            case DELETE -> minecraft.openScreen(new ConfirmScreen(this, I18n.translate("flashback.select_replay.delete_question"), replays.get(selected).path().getFileName().toString(), 0));
            case FOLDER -> Blaze3D.openPath(folder);
            case LOAD -> Flashback.openReplayFromFileBrowser();
            case SORT -> {
                var config = Flashback.getConfig();
                ReplaySorting[] values = ReplaySorting.values();
                config.internal.replaySorting = values[(config.internal.replaySorting.ordinal() + 1) % values.length];
                config.delayedSaveToDefaultFolder();
                refresh();
            }
            case DIRECTION -> {
                var config = Flashback.getConfig();
                config.internal.sortDescending = !config.internal.sortDescending;
                config.delayedSaveToDefaultFolder();
                refresh();
            }
            case OPTIONS -> minecraft.openScreen(new LegacyConfigScreen(this));
            case BACK -> minecraft.openScreen(parent);
        }
    }

    @Override
    public void confirmResult(boolean confirmed, int id) {
        if (confirmed) {
            try {
                Files.delete(replays.get(selected).path());
            } catch (IOException e) {
                FlashbackLegacy.LOGGER.error("Could not delete replay", e);
            }
        }
        minecraft.openScreen(this);
    }

    @Override
    public void handleMouse() {
        super.handleMouse();
        list.handleMouse();
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        list.render(mouseX, mouseY, tickDelta);
        drawCenteredString(textRenderer, I18n.translate("flashback.select_replay"), width / 2, 8, 0xFFFFFF);
        search.render();
        if (search.getText().isEmpty() && !search.isFocused()) drawString(textRenderer, I18n.translate("flashback.search"), width / 2 - 148, 28, 0x808080);
        super.render(mouseX, mouseY, tickDelta);
    }

    private class ReplayList extends ListWidget {
        ReplayList() {
            super(LegacySelectReplayScreen.this.minecraft, LegacySelectReplayScreen.this.width, LegacySelectReplayScreen.this.height, 46, LegacySelectReplayScreen.this.height - 60, 24);
        }

        @Override
        protected int size() {
            return replays.size();
        }

        @Override
        protected void entryClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            selected = index;
            Replay replay = replays.get(index);
            open.active = true;
            delete.active = !replay.folder();
            edit.active = replay.metadata() != null;
            if (doubleClick) openSelected();
        }

        @Override
        protected boolean isEntrySelected(int index) {
            return index == selected;
        }

        @Override
        protected void renderBackground() {
            LegacySelectReplayScreen.this.renderBackground();
        }

        @Override
        protected void renderEntry(int index, int x, int y, int height, int mouseX, int mouseY) {
            Replay replay = replays.get(index);
            String file = replay.path().getFileName().toString().replaceFirst("\\.zip$", "");
            if (replay.icon() != null) {
                minecraft.getTextureManager().bind(replay.icon());
                GlStateManager.color4f(1, 1, 1, 1);
                drawTexture(x, y, 0, 0, 20, 20, 20, 20);
            }
            drawString(textRenderer, displayName(replay) + (replay.folder() ? "/" : ""), x + 24, y + 1, 0xFFFFFF);
            drawString(textRenderer, (replay.folder() ? "" : file + "  ") + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(modified(replay.path()))), x + 24, y + 11, 0x808080);
        }
    }
}
