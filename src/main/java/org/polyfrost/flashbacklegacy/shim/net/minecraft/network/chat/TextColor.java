package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.ChatFormatting;

public record TextColor(int getValue) {
    public static TextColor fromLegacyFormat(ChatFormatting format) {
        return format.color == null ? null : new TextColor(format.color);
    }
}
