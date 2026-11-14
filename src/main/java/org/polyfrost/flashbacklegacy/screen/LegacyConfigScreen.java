package org.polyfrost.flashbacklegacy.screen;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.ComboOption;
import com.moulberry.lattice.annotation.LatticeCategory;
import com.moulberry.lattice.annotation.LatticeFormatValues;
import com.moulberry.lattice.annotation.LatticeOption;
import com.moulberry.lattice.annotation.constraint.LatticeEnableIf;
import com.moulberry.lattice.annotation.constraint.LatticeFloatRange;
import com.moulberry.lattice.annotation.constraint.LatticeIntRange;
import com.moulberry.lattice.annotation.constraint.LatticeShowIf;
import com.moulberry.lattice.annotation.widget.LatticeWidgetMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LegacyConfigScreen extends Screen {
    private static final int DONE = 0, PREVIOUS = 1, NEXT = 2, FIRST_OPTION = 3;
    private static final int LEFT = 155, ROW = 24;
    private static final String KEY_CATEGORY = "key.category.flashback.keybind";

    private record Option(Object owner, Field field, String title, String description) {
        Object get() {
            return read(field, owner);
        }

        void set(Object value) {
            try {
                field.set(owner, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private record Page(String name, Object owner, Field[] fields, boolean enabled) {
    }

    private final Screen parent;
    private final List<Page> pages = new ArrayList<>();
    private final Map<ButtonWidget, Option> widgets = new LinkedHashMap<>();
    private final List<TextEntry> textEntries = new ArrayList<>();
    private final Map<ButtonWidget, KeyBinding> keys = new LinkedHashMap<>();
    private final List<String> messages = new ArrayList<>();
    private KeyBinding listening;
    private int page, messageY;

    private record TextEntry(TextFieldWidget widget, Option option) {
    }

    public LegacyConfigScreen(Screen parent) {
        this.parent = parent;
        loadPages();
    }

    private void loadPages() {
        pages.clear();
        addPages(Flashback.getConfig());
        pages.add(new Page(I18n.translate(KEY_CATEGORY), null, new Field[0], true));
    }

    private void addPages(Object parent) {
        for (Field field : parent.getClass().getFields()) {
            LatticeCategory category = field.getAnnotation(LatticeCategory.class);
            if (category == null) continue;
            Object owner = read(field, parent);
            Field[] fields = owner.getClass().getFields();
            boolean used = false;
            for (Field option : fields) used |= option.isAnnotationPresent(LatticeOption.class);
            LatticeEnableIf enable = field.getAnnotation(LatticeEnableIf.class);
            boolean enabled = enable == null || condition(parent, enable.function());
            if (used) pages.add(new Page(I18n.translate(category.name()), owner, fields, enabled));
            addPages(owner);
        }
    }

    private static Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean shown(Field field, Object owner) {
        LatticeShowIf show = field.getAnnotation(LatticeShowIf.class);
        return show == null || condition(owner, show.function());
    }

    private static boolean condition(Object owner, String function) {
        try {
            Method condition = owner.getClass().getDeclaredMethod(function);
            condition.setAccessible(true);
            return (boolean) condition.invoke(owner);
        } catch (ReflectiveOperationException e) {
            FlashbackLegacy.LOGGER.error("Could not evaluate option condition", e);
            return false;
        }
    }

    private static String description(LatticeOption option) {
        String key = option.description();
        if (key.isEmpty()) return "";
        return I18n.translate(key.startsWith("!!") ? option.title() + key.substring(2) : key);
    }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        widgets.clear();
        textEntries.clear();
        keys.clear();
        messages.clear();
        buttons.add(new ButtonWidget(DONE, width / 2 - 100, height - 28, I18n.translate("gui.done")));
        buttons.add(new ButtonWidget(PREVIOUS, width / 2 - LEFT, 6, 20, 20, "<"));
        buttons.add(new ButtonWidget(NEXT, width / 2 + LEFT - 20, 6, 20, 20, ">"));

        Page current = pages.get(page);
        int x = width / 2 - LEFT, y = 32, id = FIRST_OPTION;
        if (current.owner() == null) {
            for (KeyBinding key : minecraft.options.keyBindings) {
                if (!KEY_CATEGORY.equals(key.getCategory())) continue;
                ButtonWidget widget = new ButtonWidget(id++, x, y, LEFT * 2, 20, keyLabel(key));
                buttons.add(widget);
                keys.put(widget, key);
                y += ROW;
            }
        }
        for (Field field : current.fields()) {
            if (field.isAnnotationPresent(LatticeWidgetMessage.class)) {
                messages.addAll(textRenderer.split(((Component) read(field, current.owner())).getString(), LEFT * 2));
                continue;
            }
            LatticeOption annotation = field.getAnnotation(LatticeOption.class);
            if (annotation == null || !shown(field, current.owner())) continue;
            Option option = new Option(current.owner(), field, I18n.translate(annotation.title()), description(annotation));
            if (field.getType() == String.class) {
                TextFieldWidget text = new TextFieldWidget(id, textRenderer, x + LEFT + 1, y, LEFT - 2, 20);
                text.setMaxLength(1024);
                text.setText((String) option.get());
                text.setEditable(current.enabled());
                textEntries.add(new TextEntry(text, option));
            } else {
                ButtonWidget widget = field.getType() == boolean.class || field.getType().isEnum()
                    ? new CycleButton(id, x, y, option) : new Slider(id, x, y, option);
                widget.active = current.enabled();
                buttons.add(widget);
                widgets.put(widget, option);
            }
            id++;
            y += ROW;
        }
        messageY = y + 4;
    }

    private String keyLabel(KeyBinding key) {
        String name = GameOptions.getKeyName(key.getKeyCode());
        boolean conflict = key.getKeyCode() != 0 && Arrays.stream(minecraft.options.keyBindings)
            .anyMatch(other -> other != key && other.getKeyCode() == key.getKeyCode());
        if (key == listening) name = "\u00a7f> \u00a7e" + name + "\u00a7f <";
        else if (conflict) name = "\u00a7c" + name;
        return I18n.translate(key.getName()) + ": " + name;
    }

    private void bind(int code) {
        minecraft.options.setKeyCode(listening, code);
        KeyBinding.resetMapping();
        listening = null;
        loadPages();
        rebuild();
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
        Flashback.getConfig().saveToDefaultFolder();
    }

    @Override
    public void tick() {
        textEntries.forEach(entry -> entry.widget().tick());
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id == DONE) {
            minecraft.openScreen(parent);
        } else if (button.id == PREVIOUS || button.id == NEXT) {
            page = (page + (button.id == NEXT ? 1 : pages.size() - 1)) % pages.size();
            rebuild();
        } else if (keys.containsKey(button)) {
            listening = keys.get(button);
            rebuild();
        } else if (button instanceof CycleButton cycle) {
            cycle.next();
            rebuild();
        }
    }

    private void rebuild() {
        buttons.clear();
        init();
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (listening != null) {
            bind(key == Keyboard.KEY_ESCAPE ? 0 : key != 0 ? key : chr + 256);
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            minecraft.openScreen(parent);
            return;
        }
        for (TextEntry entry : textEntries) {
            if (entry.widget().keyPressed(chr, key)) entry.option().set(entry.widget().getText());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (listening != null) {
            bind(button - 100);
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        textEntries.forEach(entry -> entry.widget().mouseClicked(mouseX, mouseY, button));
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        renderBackground();
        drawCenteredString(textRenderer, pages.get(page).name(), width / 2, 12, 0xFFFFFF);
        for (TextEntry entry : textEntries) {
            drawString(textRenderer, entry.option().title(), width / 2 - LEFT, entry.widget().y + 6, 0xE0E0E0);
            entry.widget().render();
        }
        int y = messageY;
        for (String line : messages) {
            drawString(textRenderer, line, width / 2 - LEFT, y, 0xFF8080);
            y += textRenderer.fontHeight;
        }
        super.render(mouseX, mouseY, tickDelta);
        widgets.forEach((widget, option) -> {
            if (widget.isHovered()) tooltip(option.description(), mouseX, mouseY);
        });
        for (TextEntry entry : textEntries) {
            TextFieldWidget widget = entry.widget();
            boolean over = mouseX >= width / 2 - LEFT && mouseX < width / 2 + LEFT && mouseY >= widget.y && mouseY < widget.y + 20;
            if (over) tooltip(entry.option().description(), mouseX, mouseY);
        }
    }

    private void tooltip(String text, int mouseX, int mouseY) {
        if (text.isEmpty()) return;
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) lines.addAll(textRenderer.split(line, 250));
        renderTooltip(lines, mouseX, mouseY);
    }

    private static class CycleButton extends ButtonWidget {
        private final Option option;

        CycleButton(int id, int x, int y, Option option) {
            super(id, x, y, LEFT * 2, 20, "");
            this.option = option;
            label();
        }

        void next() {
            Object value = option.get();
            if (value instanceof Boolean on) {
                option.set(!on);
            } else {
                Object[] values = option.field().getType().getEnumConstants();
                option.set(values[(((Enum<?>) value).ordinal() + 1) % values.length]);
            }
            label();
        }

        private void label() {
            Object value = option.get();
            String text = value instanceof Boolean on ? I18n.translate(on ? "options.on" : "options.off") : ((ComboOption) value).text();
            message = option.title() + ": " + text;
        }
    }

    private static class Slider extends ButtonWidget {
        private final Option option;
        private final float min, max, step;
        private boolean dragging;

        Slider(int id, int x, int y, Option option) {
            super(id, x, y, LEFT * 2, 20, "");
            this.option = option;
            LatticeIntRange ints = option.field().getAnnotation(LatticeIntRange.class);
            LatticeFloatRange floats = option.field().getAnnotation(LatticeFloatRange.class);
            min = ints != null ? ints.min() : floats.min();
            max = ints != null ? ints.max() : floats.max();
            step = ints != null ? Math.max(1, ints.step()) : 0;
            label();
        }

        private float value() {
            return ((Number) option.get()).floatValue();
        }

        private void label() {
            Object value = option.get();
            String text = value instanceof Float number ? String.format("%.2f", number) : value.toString();
            LatticeFormatValues format = option.field().getAnnotation(LatticeFormatValues.class);
            if (format != null) text = format.translate() ? I18n.translate(format.formattingString(), text) : String.format(format.formattingString(), text);
            message = option.title() + ": " + text;
        }

        private void drag(int mouseX) {
            float raw = min + Math.clamp((mouseX - (x + 4)) / (float) (width - 8), 0f, 1f) * (max - min);
            if (step > 0) raw = min + Math.round((raw - min) / step) * step;
            if (option.field().getType() == int.class) option.set(Math.round(raw));
            else option.set(raw);
            label();
        }

        @Override
        protected int getYImage(boolean hovered) {
            return 0;
        }

        @Override
        protected void renderBackground(Minecraft minecraft, int mouseX, int mouseY) {
            if (!visible) return;
            if (dragging) drag(mouseX);
            GlStateManager.color4f(1f, 1f, 1f, 1f);
            int knob = x + Math.round(Math.clamp((value() - min) / (max - min), 0f, 1f) * (width - 8));
            drawTexture(knob, y, 0, 66, 4, 20);
            drawTexture(knob + 4, y, 196, 66, 4, 20);
        }

        @Override
        public boolean mouseClicked(Minecraft minecraft, int mouseX, int mouseY) {
            if (!super.mouseClicked(minecraft, mouseX, mouseY)) return false;
            dragging = true;
            drag(mouseX);
            return true;
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY) {
            dragging = false;
        }
    }
}
