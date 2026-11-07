package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat;

import com.mojang.brigadier.Message;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public interface Component extends Message {
    Text text();

    @Override
    default String getString() {
        return text().getString();
    }

    static MutableComponent empty() {
        return literal("");
    }

    static MutableComponent literal(String text) {
        return new MutableComponent(new LiteralText(text));
    }

    static MutableComponent translatable(String key) {
        return new MutableComponent(new TranslatableText(key));
    }

    static MutableComponent translatableEscape(String key, Object... args) {
        return translatable(key, args);
    }

    static MutableComponent translatable(String key, Object... args) {
        Object[] legacy = new Object[args.length];
        for (int i = 0; i < args.length; i++) legacy[i] = args[i] instanceof Component c ? c.text() : args[i];
        return new MutableComponent(new TranslatableText(key, legacy));
    }
}
