package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.gui.components.toasts;

import net.minecraft.client.Minecraft;
import net.minecraft.text.LiteralText;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;

public class SystemToast implements Toast {
    public static void add(ToastManager manager, SystemToastId id, Component title, Component message) {
        LiteralText text = new LiteralText("");
        text.append(title.text()).append(": ");
        if (message != null) text.append(message.text());
        Minecraft.getInstance().gui.getChat().addMessage(text);
    }

    public static class SystemToastId {
        public SystemToastId() {
        }

        public SystemToastId(long displayTime) {
        }
    }
}
