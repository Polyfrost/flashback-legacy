package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat;

import net.minecraft.text.Formatting;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.ChatFormatting;

public final class MutableComponent implements Component {
    private final Text text;

    MutableComponent(Text text) {
        this.text = text;
    }

    @Override
    public Text text() {
        return text;
    }

    public MutableComponent append(Component other) {
        text.append(other.text());
        return this;
    }

    public MutableComponent append(String other) {
        text.append(other);
        return this;
    }

    public MutableComponent withStyle(ChatFormatting... formats) {
        for (ChatFormatting format : formats) withStyle(format);
        return this;
    }

    public MutableComponent withStyle(ChatFormatting format) {
        Style style = text.getStyle();
        switch (format) {
            case OBFUSCATED -> style.setObfuscated(true);
            case BOLD -> style.setBold(true);
            case STRIKETHROUGH -> style.setStrikethrough(true);
            case UNDERLINE -> style.setUnderlined(true);
            case ITALIC -> style.setItalic(true);
            default -> style.setColor(Formatting.byName(format.name()));
        }
        return this;
    }
}
